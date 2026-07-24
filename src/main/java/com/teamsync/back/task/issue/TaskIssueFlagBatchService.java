package com.teamsync.back.task.issue;

import com.teamsync.back.notification.NotificationService;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskDependency;
import com.teamsync.back.task.TaskDependencyRepository;
import com.teamsync.back.task.TaskRepository;
import com.teamsync.back.task.TaskStatus;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-406(이슈/리스크 자동 플래그, 완전판) 배치의 프로젝트 1건 처리를 전담한다.
 * TaskIssueFlagSchedulerService와 별도 빈으로 분리한 이유는 RecurringTaskGenerationService와 동일:
 * {@link Transactional}의 REQUIRES_NEW는 프록시를 통한 외부 호출에서만 적용되므로(Spring AOP
 * self-invocation 한계), 프로젝트별 트랜잭션을 실제로 격리하려면 별도 컴포넌트가 필요하다.
 * 한 프로젝트 처리 중 예외가 발생해도 그 프로젝트의 변경분만 롤백되고, 이미 처리된/이후 처리될
 * 다른 프로젝트에는 영향을 주지 않는다(FR-106과 동일 원칙. 이번 배치는 프로젝트 단위까지만
 * 격리하며, 같은 프로젝트 내 여러 태스크 중 하나의 처리 실패가 그 프로젝트의 나머지 태스크
 * 변경까지 되돌릴 수 있다는 점은 FR-106 템플릿 단위 격리와 동일한 수준의 절충이다).
 */
@Service
public class TaskIssueFlagBatchService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int STALE_DAYS_THRESHOLD = 21;

	private final ProjectRepository projectRepository;
	private final TaskRepository taskRepository;
	private final TaskIssueFlagRepository taskIssueFlagRepository;
	private final TaskDependencyRepository taskDependencyRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;

	public TaskIssueFlagBatchService(ProjectRepository projectRepository, TaskRepository taskRepository,
			TaskIssueFlagRepository taskIssueFlagRepository, TaskDependencyRepository taskDependencyRepository,
			UserRepository userRepository, NotificationService notificationService) {
		this.projectRepository = projectRepository;
		this.taskRepository = taskRepository;
		this.taskIssueFlagRepository = taskIssueFlagRepository;
		this.taskDependencyRepository = taskDependencyRepository;
		this.userRepository = userRepository;
		this.notificationService = notificationService;
	}

	/**
	 * projectId만 받아 이 트랜잭션 안에서 프로젝트를 다시 조회하는 이유: 호출부(스케줄러)의 순회 루프는
	 * 트랜잭션 밖에서 동작하므로 넘겨받은 엔티티는 detached 상태다(RecurringTaskGenerationService와
	 * 동일한 이유).
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recomputeForProject(Long projectId) {
		Project project = projectRepository.findById(projectId).orElse(null);
		if (project == null) {
			return; // 배치 실행 사이 프로젝트가 삭제된 경우.
		}
		Long workspaceId = project.getWorkspace().getId();

		LocalDate today = LocalDate.now(KST);
		LocalDateTime staleThreshold = LocalDateTime.now(KST).minusDays(STALE_DAYS_THRESHOLD);

		List<Task> openTasks = taskRepository.findAllByProject_IdAndStatusNotOrderByIdAsc(projectId, TaskStatus.DONE);
		List<TaskIssueFlag> openFlags = taskIssueFlagRepository.findAllByTask_Project_IdAndStatus(projectId,
				TaskIssueStatus.OPEN);

		Map<Long, Map<TaskIssueKind, TaskIssueFlag>> openFlagsByTaskId = new HashMap<>();
		for (TaskIssueFlag flag : openFlags) {
			openFlagsByTaskId.computeIfAbsent(flag.getTask().getId(), key -> new EnumMap<>(TaskIssueKind.class))
					.put(flag.getKind(), flag);
		}

		// LEADER/ADMIN은 프로젝트 전체에 공통이므로 한 번만 조회해 재사용한다.
		List<User> leaderAndAdmins = userRepository.findAllByWorkspaceIdAndRoleIn(workspaceId,
				List.of(Role.ADMIN, Role.LEADER));

		for (Task task : openTasks) {
			List<TaskDependency> predecessorLinks = taskDependencyRepository.findBySuccessorTask_Id(task.getId());
			List<TaskIssueDetector.DetectedIssue> detected = TaskIssueDetector.detect(task, today, staleThreshold,
					predecessorLinks);

			Map<TaskIssueKind, TaskIssueFlag> existingForTask = openFlagsByTaskId.remove(task.getId());
			Set<TaskIssueKind> detectedKinds = new HashSet<>();

			for (TaskIssueDetector.DetectedIssue issue : detected) {
				detectedKinds.add(issue.kind());
				boolean alreadyOpen = existingForTask != null && existingForTask.containsKey(issue.kind());
				if (!alreadyOpen) {
					TaskIssueFlag flag = new TaskIssueFlag(task, issue.kind(), issue.detail(),
							LocalDateTime.now(KST));
					taskIssueFlagRepository.save(flag);
					notifyIssueDetected(task, issue.kind(), issue.detail(), leaderAndAdmins);
				}
			}

			// 기존 OPEN 플래그 중 이번 재계산에서 더 이상 검출되지 않은 kind는 조건이 해소된 것이므로
			// 자동 RESOLVED 처리한다(resolvedBy=null: 시스템 처리).
			if (existingForTask != null) {
				for (Map.Entry<TaskIssueKind, TaskIssueFlag> entry : existingForTask.entrySet()) {
					if (!detectedKinds.contains(entry.getKey())) {
						entry.getValue().resolve(null);
					}
				}
			}
		}

		// openTasks에 남아있지 않은(=태스크가 DONE으로 전환된) 태스크에 매달린 OPEN 플래그는 전부 조건
		// 해소이므로 자동 RESOLVED 처리한다.
		for (Map<TaskIssueKind, TaskIssueFlag> remaining : openFlagsByTaskId.values()) {
			remaining.values().forEach(flag -> flag.resolve(null));
		}
	}

	private void notifyIssueDetected(Task task, TaskIssueKind kind, String detail, List<User> leaderAndAdmins) {
		Set<User> recipients = new LinkedHashSet<>(task.getAssignees());
		recipients.addAll(leaderAndAdmins);
		notificationService.notifyTaskIssueFlagged(task, kind, detail, recipients);
	}
}
