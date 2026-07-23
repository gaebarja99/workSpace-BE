package com.teamsync.back.task;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.ChecklistItemNotFoundException;
import com.teamsync.back.common.exception.InvalidAssigneeException;
import com.teamsync.back.common.exception.InvalidTaskRequestException;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.common.exception.TaskNotFoundException;
import com.teamsync.back.notification.NotificationService;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.task.dto.ChecklistItemCreateRequest;
import com.teamsync.back.task.dto.ChecklistItemResponse;
import com.teamsync.back.task.dto.ChecklistItemUpdateRequest;
import com.teamsync.back.task.dto.MyTaskResponse;
import com.teamsync.back.task.dto.TaskCreateRequest;
import com.teamsync.back.task.dto.TaskResponse;
import com.teamsync.back.task.dto.TaskSummaryResponse;
import com.teamsync.back.task.dto.TaskUpdateRequest;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-101(보드) / FR-102(태스크 카드) 서비스.
 * ProjectService와 동일한 원칙: 클라이언트가 전달한 projectId/taskId가 요청자의 워크스페이스에
 * 실제로 속하는지 항상 principal.workspaceId() 기준으로 재검증하고, 아니면 404로 응답해
 * 다른 워크스페이스 데이터의 존재 자체를 숨긴다(PRD 5.6 리스크 대응).
 */
@Service
public class TaskService {

	private final TaskRepository taskRepository;
	private final TaskChecklistItemRepository checklistItemRepository;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;

	public TaskService(TaskRepository taskRepository, TaskChecklistItemRepository checklistItemRepository,
			ProjectRepository projectRepository, UserRepository userRepository,
			NotificationService notificationService) {
		this.taskRepository = taskRepository;
		this.checklistItemRepository = checklistItemRepository;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
		this.notificationService = notificationService;
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
		// FR-108 트리거 1(TASK_ASSIGNED): 생성 시 지정된 초기 담당자 전원(생성자 본인 제외)에게 알림.
		notificationService.notifyTaskAssigned(savedTask, savedTask.getAssignees(), principal.userId());
		return TaskResponse.from(savedTask);
	}

	@Transactional(readOnly = true)
	public List<TaskSummaryResponse> listTasks(AuthenticatedUser principal, Long projectId) {
		getProjectInWorkspace(principal, projectId);
		return taskRepository.findAllByProjectIdOrderByDueDateAscIdAsc(projectId).stream()
				.map(TaskSummaryResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public TaskResponse getTask(AuthenticatedUser principal, Long taskId) {
		return TaskResponse.from(getTaskInWorkspace(principal, taskId));
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
		// FR-108 트리거 2/3을 위해 변경 전 상태/담당자를 먼저 기억해둔다(도메인 메서드가 이전 값을
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
			// FR-108 트리거 3(TASK_STATUS_CHANGED): 실제로 값이 바뀐 경우에만, 변경 시점의 담당자
			// 전원(변경을 수행한 principal 본인 제외)에게 알림을 생성한다.
			if (request.status() != previousStatus) {
				notificationService.notifyTaskStatusChanged(task, previousStatus, request.status(),
						principal.userId());
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
			// FR-108 트리거 2(TASK_ASSIGNED): 이전 담당자 집합에 없던 "신규 추가분"에게만
			// 알림을 생성한다(기존 유지분/제거분은 대상 아님).
			Set<User> newlyAddedAssignees = resolvedAssignees.stream()
					.filter(user -> !previousAssigneeIds.contains(user.getId()))
					.collect(Collectors.toCollection(LinkedHashSet::new));
			notificationService.notifyTaskAssigned(task, newlyAddedAssignees, principal.userId());
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
}
