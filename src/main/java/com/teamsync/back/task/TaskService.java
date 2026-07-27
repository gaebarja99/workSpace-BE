package com.teamsync.back.task;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.ChecklistItemNotFoundException;
import com.teamsync.back.common.exception.InvalidAssigneeException;
import com.teamsync.back.common.exception.InvalidTaskRequestException;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.common.exception.TaskNotFoundException;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.task.dto.ChecklistItemCreateRequest;
import com.teamsync.back.task.dto.ChecklistItemResponse;
import com.teamsync.back.task.dto.ChecklistItemUpdateRequest;
import com.teamsync.back.task.dto.MyTaskResponse;
import com.teamsync.back.task.dto.TaskActivityResponse;
import com.teamsync.back.task.dto.TaskCommentRequest;
import com.teamsync.back.task.dto.TaskCommentResponse;
import com.teamsync.back.task.dto.TaskCreateRequest;
import com.teamsync.back.task.dto.TaskResponse;
import com.teamsync.back.task.dto.TaskSummaryResponse;
import com.teamsync.back.task.dto.TaskSummaryStatsResponse;
import com.teamsync.back.task.dto.TaskUpdateRequest;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-101(보드) / FR-102(태스크 카드) 서비스.
 * ProjectService와 동일한 원칙: 클라이언트가 전달한 projectId/taskId가 요청자의 워크스페이스에
 * 실제로 속하는지 항상 principal.workspaceId() 기준으로 재검증하고, 아니면 404로 응답해
 * 다른 워크스페이스 데이터의 존재 자체를 숨긴다(PRD 5.6 리스크 대응).
 */
@Slf4j
@Service
public class TaskService {

	private final TaskRepository taskRepository;
	private final TaskChecklistItemRepository checklistItemRepository;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final TaskActivityService taskActivityService;
	private final TaskActivityRepository taskActivityRepository;
	private final TaskCommentRepository taskCommentRepository;

	public TaskService(TaskRepository taskRepository, TaskChecklistItemRepository checklistItemRepository,
			ProjectRepository projectRepository, UserRepository userRepository,
			TaskActivityService taskActivityService, TaskActivityRepository taskActivityRepository,
			TaskCommentRepository taskCommentRepository) {
		this.taskRepository = taskRepository;
		this.checklistItemRepository = checklistItemRepository;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
		this.taskActivityService = taskActivityService;
		this.taskActivityRepository = taskActivityRepository;
		this.taskCommentRepository = taskCommentRepository;
	}

	@Transactional
	public TaskResponse createTask(AuthenticatedUser principal, Long projectId, TaskCreateRequest request) {
		Project project = getProjectInWorkspace(principal, projectId);
		Set<User> assignees = resolveAssignees(principal, request.assigneeIds());
		User createdBy = userRepository.getReferenceById(principal.userId());

		Task task = new Task(
				project,
				request.title().trim(),
				request.description(),
				request.priority() != null ? request.priority() : TaskPriority.MEDIUM,
				request.status() != null ? request.status() : TaskStatus.TODO,
				request.startDate(),
				request.dueDate(),
				createdBy,
				assignees);

		Task savedTask = taskRepository.save(task);
		// FR-105-B: 태스크 생성 활동 기록(actor=생성자).
		taskActivityService.recordCreated(savedTask, createdBy);
		return TaskResponse.from(savedTask);
	}

	@Transactional(readOnly = true)
	public List<TaskSummaryResponse> listTasks(AuthenticatedUser principal, Long projectId) {
		getProjectInWorkspace(principal, projectId);
		return taskRepository.findAllByProjectIdOrderByDueDateAscIdAsc(projectId).stream()
				.map(TaskSummaryResponse::from)
				.toList();
	}

	// FR-101/FR-102(보드 상태 집계 요약): 프로젝트에 속한 태스크를 status별로 카운트하고
	// 완료 비율(progressPercent)을 계산한다. 조회 권한은 listTasks와 동일(워크스페이스 소속이면
	// GUEST 포함 누구나 조회 가능)하다.
	@Transactional(readOnly = true)
	public TaskSummaryStatsResponse getTaskSummaryStats(AuthenticatedUser principal, Long projectId) {
		getProjectInWorkspace(principal, projectId);
		long todo = taskRepository.countByProject_IdAndStatus(projectId, TaskStatus.TODO);
		long inProgress = taskRepository.countByProject_IdAndStatus(projectId, TaskStatus.IN_PROGRESS);
		long review = taskRepository.countByProject_IdAndStatus(projectId, TaskStatus.REVIEW);
		long done = taskRepository.countByProject_IdAndStatus(projectId, TaskStatus.DONE);
		return TaskSummaryStatsResponse.of(todo, inProgress, review, done);
	}

	@Transactional(readOnly = true)
	public TaskResponse getTask(AuthenticatedUser principal, Long taskId) {
		Task task = getTaskInWorkspace(principal, taskId);
		return TaskResponse.from(task);
	}

	/**
	 * FR-104(담당자별 대시보드, US-01 "내 업무"): 현재 사용자가 담당자로 지정된, 완료되지 않은
	 * 태스크를 dueDate ASC(null은 마지막) → priority(URGENT>HIGH>MEDIUM>LOW) → id ASC 순으로 반환한다.
	 */
	@Transactional(readOnly = true)
	public List<MyTaskResponse> listMyTasks(AuthenticatedUser principal) {
		Comparator<Task> byDueDateThenPriorityThenId = Comparator
				.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(Task::getPriority)
				.thenComparing(Task::getId);

		return taskRepository
				.findAllByAssignees_IdAndProject_Workspace_IdAndStatusNotOrderByDueDateAscIdAsc(
						principal.userId(), principal.workspaceId(), TaskStatus.DONE)
				.stream()
				.sorted(byDueDateThenPriorityThenId)
				.map(MyTaskResponse::from)
				.toList();
	}

	@Transactional
	public TaskResponse updateTask(AuthenticatedUser principal, Long taskId, TaskUpdateRequest request) {
		Task task = getTaskInWorkspace(principal, taskId);
		// FR-105-B를 위해 변경 전 상태/담당자를 먼저 기억해둔다(도메인 메서드가 이전 값을
		// 남기지 않으므로, diff는 서비스 계층에서 변경 직전에 스냅샷을 떠 계산해야 한다).
		TaskStatus previousStatus = task.getStatus();
		Set<Long> previousAssigneeIds = task.getAssignees().stream()
				.map(User::getId)
				.collect(Collectors.toSet());

		if (request.title() != null) {
			String trimmed = request.title().trim();
			if (trimmed.isEmpty()) {
				throw new InvalidTaskRequestException("태스크 제목은 공백일 수 없습니다.");
			}
			task.changeTitle(trimmed);
		}
		if (request.description() != null) {
			task.changeDescription(request.description());
		}
		if (request.priority() != null) {
			task.changePriority(request.priority());
		}
		if (request.status() != null) {
			task.changeStatus(request.status());
			if (request.status() != previousStatus) {
				// FR-105-B: 상태 변경 활동 기록(detail 예: "진행 중 → 검토").
				taskActivityService.recordStatusChanged(task, previousStatus, request.status(),
						userRepository.getReferenceById(principal.userId()));
			}
		}
		if (request.startDate() != null) {
			task.changeStartDate(request.startDate());
		}
		if (request.dueDate() != null) {
			task.changeDueDate(request.dueDate());
		}
		if (request.assigneeIds() != null) {
			if (request.assigneeIds().isEmpty()) {
				throw new InvalidTaskRequestException("담당자는 최소 1명 이상이어야 합니다.");
			}
			Set<User> resolvedAssignees = resolveAssignees(principal, request.assigneeIds());
			task.changeAssignees(resolvedAssignees);
			// FR-105-B: 담당자 집합이 실제로 바뀐 경우에만 활동을 기록한다(변경 없는 재지정은 로그 남기지 않음).
			Set<Long> resolvedAssigneeIds = resolvedAssignees.stream().map(User::getId).collect(Collectors.toSet());
			if (!resolvedAssigneeIds.equals(previousAssigneeIds)) {
				taskActivityService.recordAssigneeChanged(task, resolvedAssignees,
						userRepository.getReferenceById(principal.userId()));
			}
		}

		return TaskResponse.from(task);
	}

	@Transactional
	public void deleteTask(AuthenticatedUser principal, Long taskId) {
		Task task = getTaskInWorkspace(principal, taskId);
		taskRepository.delete(task);
	}

	@Transactional
	public ChecklistItemResponse addChecklistItem(AuthenticatedUser principal, Long taskId,
			ChecklistItemCreateRequest request) {
		Task task = getTaskInWorkspace(principal, taskId);
		int nextPosition = (int) checklistItemRepository.countByTaskId(task.getId());
		TaskChecklistItem item = checklistItemRepository.save(
				new TaskChecklistItem(task, request.content().trim(), nextPosition));
		return ChecklistItemResponse.from(item);
	}

	@Transactional
	public ChecklistItemResponse updateChecklistItem(AuthenticatedUser principal, Long taskId, Long itemId,
			ChecklistItemUpdateRequest request) {
		getTaskInWorkspace(principal, taskId);
		TaskChecklistItem item = checklistItemRepository.findByIdAndTaskId(itemId, taskId)
				.orElseThrow(ChecklistItemNotFoundException::new);

		if (request.content() != null) {
			String trimmed = request.content().trim();
			if (trimmed.isEmpty()) {
				throw new InvalidTaskRequestException("체크리스트 항목 내용은 공백일 수 없습니다.");
			}
			item.changeContent(trimmed);
		}
		if (request.isChecked() != null) {
			item.changeChecked(request.isChecked());
		}

		return ChecklistItemResponse.from(item);
	}

	@Transactional
	public void deleteChecklistItem(AuthenticatedUser principal, Long taskId, Long itemId) {
		getTaskInWorkspace(principal, taskId);
		TaskChecklistItem item = checklistItemRepository.findByIdAndTaskId(itemId, taskId)
				.orElseThrow(ChecklistItemNotFoundException::new);
		checklistItemRepository.delete(item);
	}

	// ----- FR-305(US-10): 태스크 댓글 -----

	@Transactional(readOnly = true)
	public List<TaskCommentResponse> listTaskComments(AuthenticatedUser principal, Long taskId) {
		getTaskInWorkspace(principal, taskId);
		return taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
				.map(TaskCommentResponse::from)
				.toList();
	}

	@Transactional
	public TaskCommentResponse createTaskComment(AuthenticatedUser principal, Long taskId,
			TaskCommentRequest request) {
		Task task = getTaskInWorkspace(principal, taskId);
		String content = request.content().trim();
		if (content.isEmpty()) {
			throw new InvalidTaskRequestException("댓글 내용은 공백일 수 없습니다.");
		}
		User author = userRepository.getReferenceById(principal.userId());
		// FR-105-A: 언급 대상 중 워크스페이스 소속인 사용자만 남긴다(그 외 id는 무시). content는 원문 그대로 저장.
		Set<User> mentionedUsers = resolveMentionedUsers(principal, request.mentionedUserIds());

		TaskComment comment = taskCommentRepository.save(new TaskComment(task, author, content, mentionedUsers));

		// FR-105-B: 댓글 작성 활동 기록.
		taskActivityService.recordCommentAdded(task, author);

		return TaskCommentResponse.from(comment);
	}

	// ----- FR-105-B(US-01): 태스크 활동 로그 조회 -----

	@Transactional(readOnly = true)
	public List<TaskActivityResponse> listTaskActivities(AuthenticatedUser principal, Long taskId) {
		getTaskInWorkspace(principal, taskId);
		return taskActivityRepository.findByTaskIdOrderByCreatedAtAscIdAsc(taskId).stream()
				.map(TaskActivityResponse::from)
				.toList();
	}

	private Project getProjectInWorkspace(AuthenticatedUser principal, Long projectId) {
		return projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
	}

	private Task getTaskInWorkspace(AuthenticatedUser principal, Long taskId) {
		return taskRepository.findByIdAndProject_Workspace_Id(taskId, principal.workspaceId())
				.orElseThrow(TaskNotFoundException::new);
	}

	private Set<User> resolveAssignees(AuthenticatedUser principal, List<Long> assigneeIds) {
		Set<Long> distinctIds = new LinkedHashSet<>(assigneeIds);
		List<User> users = userRepository.findAllByIdInAndWorkspaceId(distinctIds, principal.workspaceId());
		if (users.size() != distinctIds.size()) {
			throw new InvalidAssigneeException();
		}
		return new LinkedHashSet<>(users);
	}

	/**
	 * FR-105-A: 댓글 mentionedUserIds 중 요청자의 워크스페이스에 실제로 속한 사용자만 남긴다(그 외 id는 조용히 무시).
	 * 담당자 해석(resolveAssignees)과 달리 존재하지 않는 id가 섞여도 예외를 던지지 않는다(멘션은 best-effort).
	 */
	private Set<User> resolveMentionedUsers(AuthenticatedUser principal, List<Long> mentionedUserIds) {
		if (mentionedUserIds == null || mentionedUserIds.isEmpty()) {
			return new LinkedHashSet<>();
		}
		Set<Long> distinctIds = new LinkedHashSet<>(mentionedUserIds);
		distinctIds.remove(principal.userId()); // 작성자 자기 자신 멘션은 목록에서 제외
		return new LinkedHashSet<>(userRepository.findAllByIdInAndWorkspaceId(distinctIds, principal.workspaceId()));
	}
}
