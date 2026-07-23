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
import com.teamsync.back.task.dto.TaskCreateRequest;
import com.teamsync.back.task.dto.TaskResponse;
import com.teamsync.back.task.dto.TaskSummaryResponse;
import com.teamsync.back.task.dto.TaskUpdateRequest;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

	public TaskService(TaskRepository taskRepository, TaskChecklistItemRepository checklistItemRepository,
			ProjectRepository projectRepository, UserRepository userRepository) {
		this.taskRepository = taskRepository;
		this.checklistItemRepository = checklistItemRepository;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
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

		return TaskResponse.from(taskRepository.save(task));
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

	@Transactional
	public TaskResponse updateTask(AuthenticatedUser principal, Long taskId, TaskUpdateRequest request) {
		Task task = getTaskInWorkspace(principal, taskId);

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
			task.changeAssignees(resolveAssignees(principal, request.assigneeIds()));
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
