package com.teamsync.back.task;

import com.teamsync.back.archive.file.ArchivedFile;
import com.teamsync.back.archive.file.ArchivedFileRepository;
import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.channel.Channel;
import com.teamsync.back.channel.ChannelService;
import com.teamsync.back.channel.message.Message;
import com.teamsync.back.channel.message.MessageRepository;
import com.teamsync.back.common.exception.ArchivedFileNotFoundException;
import com.teamsync.back.common.exception.ChecklistItemNotFoundException;
import com.teamsync.back.common.exception.InvalidAssigneeException;
import com.teamsync.back.common.exception.InvalidMessageRequestException;
import com.teamsync.back.common.exception.InvalidTaskRequestException;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.common.exception.TaskFileLinkNotFoundException;
import com.teamsync.back.common.exception.TaskNotFoundException;
import com.teamsync.back.notification.NotificationService;
import com.teamsync.back.notification.TaskStatusLabels;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.task.dto.ChecklistItemCreateRequest;
import com.teamsync.back.task.dto.ChecklistItemResponse;
import com.teamsync.back.task.dto.ChecklistItemUpdateRequest;
import com.teamsync.back.task.dto.ConvertToTaskRequest;
import com.teamsync.back.task.dto.MyTaskResponse;
import com.teamsync.back.task.dto.TaskCommentRequest;
import com.teamsync.back.task.dto.TaskCommentResponse;
import com.teamsync.back.task.dto.TaskCreateRequest;
import com.teamsync.back.task.dto.TaskFileLinkRequest;
import com.teamsync.back.task.dto.TaskFileLinkResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-101(보드) / FR-102(태스크 카드) / FR-301~305(메시지↔태스크 연동, US-09/10) 서비스.
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
	private final NotificationService notificationService;
	private final TaskMessageLinkRepository taskMessageLinkRepository;
	private final TaskFileLinkRepository taskFileLinkRepository;
	private final TaskCommentRepository taskCommentRepository;
	private final ArchivedFileRepository archivedFileRepository;
	private final MessageRepository messageRepository;
	private final ChannelService channelService;

	public TaskService(TaskRepository taskRepository, TaskChecklistItemRepository checklistItemRepository,
			ProjectRepository projectRepository, UserRepository userRepository,
			NotificationService notificationService, TaskMessageLinkRepository taskMessageLinkRepository,
			TaskFileLinkRepository taskFileLinkRepository, TaskCommentRepository taskCommentRepository,
			ArchivedFileRepository archivedFileRepository, MessageRepository messageRepository,
			ChannelService channelService) {
		this.taskRepository = taskRepository;
		this.checklistItemRepository = checklistItemRepository;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
		this.notificationService = notificationService;
		this.taskMessageLinkRepository = taskMessageLinkRepository;
		this.taskFileLinkRepository = taskFileLinkRepository;
		this.taskCommentRepository = taskCommentRepository;
		this.archivedFileRepository = archivedFileRepository;
		this.messageRepository = messageRepository;
		this.channelService = channelService;
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
		// FR-302: 일반 생성 태스크는 TaskMessageLink가 없으므로 프로젝트 기본(general) 채널에 게시된다.
		publishTaskCreatedMessage(savedTask);
		return TaskResponse.from(savedTask, null);
	}

	/**
	 * FR-301(US-09): 채널 메시지를 태스크로 즉시 전환한다. 단일 트랜잭션 안에서 Task 생성 →
	 * TaskMessageLink 생성 → FR-302 시스템 메시지 게시까지 처리하며, 이 트랜잭션이 실패하면 전부
	 * 롤백된다. 이 메서드는 messages 테이블의 어떤 컬럼도 변경하지 않으므로(참조만 생성) 실패 시에도
	 * 원본 메시지는 항상 그대로 유지된다(PRD 5.6 "메시지는 유지, 태스크는 참조만 생성" 원칙).
	 */
	@Transactional
	public TaskResponse convertMessageToTask(AuthenticatedUser principal, Long channelId, Long messageId,
			ConvertToTaskRequest request) {
		Message message = channelService.getMessageInChannel(principal, channelId, messageId);
		if (taskMessageLinkRepository.existsByMessage_Id(message.getId())) {
			throw new InvalidMessageRequestException("이미 태스크로 전환된 메시지입니다.");
		}

		Project project = message.getChannel().getProject();
		Set<User> assignees = resolveAssignees(principal, request.assigneeIds());
		User createdBy = userRepository.getReferenceById(principal.userId());

		String title = hasText(request.title()) ? request.title().trim() : truncate(message.getContent(), 80);
		String description = hasText(request.description()) ? request.description() : message.getContent();
		TaskPriority priority = request.priority() != null ? request.priority() : TaskPriority.MEDIUM;

		Task task = new Task(project, title, description, priority, TaskStatus.TODO, null, request.dueDate(),
				createdBy, assignees);
		Task savedTask = taskRepository.save(task);

		TaskMessageLink link = taskMessageLinkRepository.save(new TaskMessageLink(savedTask, message));

		notificationService.notifyTaskAssigned(savedTask, savedTask.getAssignees(), principal.userId());
		// FR-302: TaskMessageLink가 이미 만들어졌으므로(위 라인) resolveNotificationChannel이 이 메시지의
		// 채널을 찾아 같은 채널에 "새 태스크가 생성되었습니다" 시스템 메시지를 게시한다.
		publishTaskCreatedMessage(savedTask);

		return TaskResponse.from(savedTask, link);
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
		Task task = getTaskInWorkspace(principal, taskId);
		return TaskResponse.from(task, taskMessageLinkRepository.findByTaskId(taskId).orElse(null));
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
				// FR-302: 완료(DONE)로의 변경은 전용 완료 메시지만 게시하고, 그 외 상태 변경은 일반
				// 상태 변경 메시지를 게시한다(중복 게시 금지).
				publishTaskStatusChangedMessage(task, request.status());
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
		if (request.channelNotificationsEnabled() != null) {
			task.changeChannelNotificationsEnabled(request.channelNotificationsEnabled());
		}

		TaskMessageLink link = taskMessageLinkRepository.findByTaskId(taskId).orElse(null);
		return TaskResponse.from(task, link);
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

	// ----- FR-304(US-09): 태스크 첨부파일 링크 -----

	@Transactional(readOnly = true)
	public List<TaskFileLinkResponse> listTaskFiles(AuthenticatedUser principal, Long taskId) {
		getTaskInWorkspace(principal, taskId);
		return taskFileLinkRepository.findByTaskId(taskId).stream().map(TaskFileLinkResponse::from).toList();
	}

	@Transactional
	public TaskFileLinkResponse linkTaskFile(AuthenticatedUser principal, Long taskId, TaskFileLinkRequest request) {
		Task task = getTaskInWorkspace(principal, taskId);
		ArchivedFile archivedFile = archivedFileRepository
				.findByIdAndProject_Workspace_Id(request.archivedFileId(), principal.workspaceId())
				.orElseThrow(ArchivedFileNotFoundException::new);
		if (!archivedFile.getProject().getId().equals(task.getProject().getId())) {
			throw new InvalidTaskRequestException("태스크와 다른 프로젝트의 파일은 연결할 수 없습니다.");
		}
		if (taskFileLinkRepository.existsByTaskIdAndArchivedFileId(taskId, archivedFile.getId())) {
			throw new InvalidTaskRequestException("이미 연결된 파일입니다.");
		}

		User linkedBy = userRepository.getReferenceById(principal.userId());
		TaskFileLink link = taskFileLinkRepository.save(new TaskFileLink(task, archivedFile, linkedBy));
		return TaskFileLinkResponse.from(link);
	}

	@Transactional
	public void unlinkTaskFile(AuthenticatedUser principal, Long taskId, Long archivedFileId) {
		getTaskInWorkspace(principal, taskId);
		if (!taskFileLinkRepository.existsByTaskIdAndArchivedFileId(taskId, archivedFileId)) {
			throw new TaskFileLinkNotFoundException();
		}
		taskFileLinkRepository.deleteByTaskIdAndArchivedFileId(taskId, archivedFileId);
	}

	// ----- FR-305(US-10): 태스크 댓글 + 채널 동기화 -----

	@Transactional(readOnly = true)
	public List<TaskCommentResponse> listTaskComments(AuthenticatedUser principal, Long taskId) {
		getTaskInWorkspace(principal, taskId);
		return taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
				.map(TaskCommentResponse::from)
				.toList();
	}

	/**
	 * 댓글 저장과 채널 동기화(TaskMessageLink가 있는 경우)를 하나의 트랜잭션으로 처리한다.
	 * 댓글 자체는 신규 데이터이므로(보존해야 할 원본이 없음) 실패 시 단순히 전체 롤백하는
	 * all-or-nothing으로 충분하다(별도 재시도 큐 불필요, 계약 문서 FR-305 참고).
	 */
	@Transactional
	public TaskCommentResponse createTaskComment(AuthenticatedUser principal, Long taskId,
			TaskCommentRequest request) {
		Task task = getTaskInWorkspace(principal, taskId);
		String content = request.content().trim();
		if (content.isEmpty()) {
			throw new InvalidTaskRequestException("댓글 내용은 공백일 수 없습니다.");
		}
		User author = userRepository.getReferenceById(principal.userId());

		TaskComment comment = taskCommentRepository.save(new TaskComment(task, author, content));

		taskMessageLinkRepository.findByTaskId(taskId).ifPresent(link -> {
			Message parentMessage = link.getMessage();
			messageRepository.save(
					Message.createTaskCommentSync(parentMessage.getChannel(), author, content, parentMessage));
		});

		return TaskCommentResponse.from(comment);
	}

	// ----- FR-302: 태스크 생성/상태변경/완료 시 채널 시스템 메시지 자동 게시 -----

	/**
	 * 대상 채널 결정: task.channelNotificationsEnabled == false면 아무것도 하지 않고, 그 외에는
	 * (1) TaskMessageLink가 있으면 그 메시지의 채널, (2) 없으면 프로젝트 기본(general) 채널을 쓴다.
	 * general 채널도 찾지 못하면 조용히 스킵한다(알림 실패가 태스크 생성/수정 자체를 막지 않는다).
	 * 채널 게시 자체가 실패하더라도(예: 예상치 못한 런타임 예외) 태스크 트랜잭션은 그대로 커밋되어야
	 * 하므로 이 두 publish 메서드는 예외를 삼키고 로그만 남긴다.
	 */
	private void publishTaskCreatedMessage(Task task) {
		try {
			Channel channel = resolveNotificationChannel(task);
			if (channel == null) {
				return;
			}
			messageRepository.save(
					Message.createSystemMessage(channel, "🆕 새 태스크가 생성되었습니다: " + task.getTitle(), null));
		} catch (RuntimeException e) {
			log.warn("FR-302 시스템 메시지(생성) 게시에 실패했습니다. taskId={}", task.getId(), e);
		}
	}

	private void publishTaskStatusChangedMessage(Task task, TaskStatus newStatus) {
		try {
			Channel channel = resolveNotificationChannel(task);
			if (channel == null) {
				return;
			}
			String content = newStatus == TaskStatus.DONE
					? "✅ 태스크가 완료되었습니다: " + task.getTitle()
					: "🔄 태스크 상태가 변경되었습니다: " + task.getTitle() + " → " + TaskStatusLabels.of(newStatus);
			messageRepository.save(Message.createSystemMessage(channel, content, null));
		} catch (RuntimeException e) {
			log.warn("FR-302 시스템 메시지(상태 변경) 게시에 실패했습니다. taskId={}", task.getId(), e);
		}
	}

	private Channel resolveNotificationChannel(Task task) {
		if (!task.isChannelNotificationsEnabled()) {
			return null;
		}
		return taskMessageLinkRepository.findByTaskId(task.getId())
				.map(link -> link.getMessage().getChannel())
				.orElseGet(() -> channelService.findDefaultChannel(task.getProject().getId()).orElse(null));
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private static String truncate(String content, int maxLength) {
		String trimmed = content.trim();
		return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
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
