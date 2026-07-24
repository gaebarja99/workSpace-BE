package com.teamsync.back.task.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamsync.back.notification.NotificationService;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskDependencyRepository;
import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.TaskRepository;
import com.teamsync.back.task.TaskStatus;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.workspace.Workspace;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * FR-406 배치(TaskIssueFlagBatchService) 단위 테스트. 신규 검출 시 생성+알림, 재실행 시 중복
 * OPEN 플래그 미생성, 조건 해소 시 자동 RESOLVED 처리를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class TaskIssueFlagBatchServiceTest {

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private TaskIssueFlagRepository taskIssueFlagRepository;

	@Mock
	private TaskDependencyRepository taskDependencyRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private NotificationService notificationService;

	private TaskIssueFlagBatchService batchService;
	private Workspace workspace;
	private Project project;

	private static final LocalDate TODAY = LocalDate.now();

	@BeforeEach
	void setUp() throws Exception {
		batchService = new TaskIssueFlagBatchService(projectRepository, taskRepository, taskIssueFlagRepository,
				taskDependencyRepository, userRepository, notificationService);
		workspace = new Workspace("그로우테크", "growtech.io");
		setId(workspace, 10L);
		project = new Project(workspace, "알파", "설명", null);
		setId(project, 100L);

		when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
		lenient().when(userRepository.findAllByWorkspaceIdAndRoleIn(10L, List.of(Role.ADMIN, Role.LEADER)))
				.thenReturn(List.of());
		lenient().when(taskDependencyRepository.findBySuccessorTask_Id(anyLong())).thenReturn(List.of());
	}

	@Test
	void 새로_검출된_이슈는_플래그가_생성되고_알림이_발송된다() throws Exception {
		Task overdueTask = task(1L, TaskStatus.IN_PROGRESS, TODAY.minusDays(3), TODAY.minusDays(1));
		when(taskRepository.findAllByProject_IdAndStatusNotOrderByIdAsc(100L, TaskStatus.DONE))
				.thenReturn(List.of(overdueTask));
		when(taskIssueFlagRepository.findAllByTask_Project_IdAndStatus(100L, TaskIssueStatus.OPEN))
				.thenReturn(List.of());

		batchService.recomputeForProject(100L);

		verify(taskIssueFlagRepository, times(1)).save(any(TaskIssueFlag.class));
		verify(notificationService, times(1)).notifyTaskIssueFlagged(any(Task.class), org.mockito.ArgumentMatchers.eq(TaskIssueKind.OVERDUE),
				any(String.class), any());
	}

	@Test
	void 이미_OPEN_플래그가_있으면_재실행해도_중복_생성되지_않는다() throws Exception {
		Task overdueTask = task(1L, TaskStatus.IN_PROGRESS, TODAY.minusDays(3), TODAY.minusDays(1));
		TaskIssueFlag existingFlag = new TaskIssueFlag(overdueTask, TaskIssueKind.OVERDUE, "2일 초과",
				LocalDateTime.now());

		when(taskRepository.findAllByProject_IdAndStatusNotOrderByIdAsc(100L, TaskStatus.DONE))
				.thenReturn(List.of(overdueTask));
		when(taskIssueFlagRepository.findAllByTask_Project_IdAndStatus(100L, TaskIssueStatus.OPEN))
				.thenReturn(List.of(existingFlag));

		batchService.recomputeForProject(100L);

		verify(taskIssueFlagRepository, never()).save(any(TaskIssueFlag.class));
		verify(notificationService, never()).notifyTaskIssueFlagged(any(), any(), any(), any());
		assertThat(existingFlag.getStatus()).isEqualTo(TaskIssueStatus.OPEN);
	}

	@Test
	void 태스크가_완료되어_더_이상_열린_태스크_목록에_없으면_기존_OPEN_플래그는_자동_RESOLVED된다() throws Exception {
		Task doneTask = task(2L, TaskStatus.DONE, TODAY.minusDays(3), TODAY.minusDays(1));
		TaskIssueFlag staleOpenFlag = new TaskIssueFlag(doneTask, TaskIssueKind.OVERDUE, "3일 초과",
				LocalDateTime.now());

		// 태스크가 DONE으로 전환되어 "미완료 태스크" 목록에서 더 이상 조회되지 않는다.
		when(taskRepository.findAllByProject_IdAndStatusNotOrderByIdAsc(100L, TaskStatus.DONE)).thenReturn(List.of());
		when(taskIssueFlagRepository.findAllByTask_Project_IdAndStatus(100L, TaskIssueStatus.OPEN))
				.thenReturn(List.of(staleOpenFlag));

		batchService.recomputeForProject(100L);

		assertThat(staleOpenFlag.getStatus()).isEqualTo(TaskIssueStatus.RESOLVED);
		assertThat(staleOpenFlag.getResolvedBy()).isNull(); // 시스템(배치) 처리 표시.
		verify(taskIssueFlagRepository, never()).save(any(TaskIssueFlag.class));
	}

	@Test
	void 마감일_복귀로_조건이_해소되면_해당_kind_플래그만_자동_RESOLVED되고_다른_kind는_유지된다() throws Exception {
		// 처음엔 OVERDUE였으나 이번 재계산 시점엔 마감일이 미래로 변경(연장)되어 더 이상 OVERDUE가 아니다.
		Task task = task(3L, TaskStatus.IN_PROGRESS, TODAY.plusDays(10), TODAY.minusDays(1));
		TaskIssueFlag noLongerOverdue = new TaskIssueFlag(task, TaskIssueKind.OVERDUE, "3일 초과", LocalDateTime.now());

		when(taskRepository.findAllByProject_IdAndStatusNotOrderByIdAsc(100L, TaskStatus.DONE))
				.thenReturn(List.of(task));
		when(taskIssueFlagRepository.findAllByTask_Project_IdAndStatus(100L, TaskIssueStatus.OPEN))
				.thenReturn(List.of(noLongerOverdue));

		batchService.recomputeForProject(100L);

		assertThat(noLongerOverdue.getStatus()).isEqualTo(TaskIssueStatus.RESOLVED);
		verify(taskIssueFlagRepository, never()).save(any(TaskIssueFlag.class));
		verify(notificationService, never()).notifyTaskIssueFlagged(any(), any(), any(), any());
	}

	private Task task(Long id, TaskStatus status, LocalDate dueDate, LocalDate updatedAtDate) throws Exception {
		Task task = new Task(project, "태스크" + id, "설명", TaskPriority.MEDIUM, status, null, dueDate, null,
				Set.of());
		setId(task, id);
		setUpdatedAt(task, updatedAtDate.atStartOfDay());
		return task;
	}

	private void setId(Object entity, Long id) throws Exception {
		Field idField = findIdField(entity.getClass());
		idField.setAccessible(true);
		idField.set(entity, id);
	}

	private Field findIdField(Class<?> type) throws NoSuchFieldException {
		Class<?> current = type;
		while (current != null) {
			try {
				return current.getDeclaredField("id");
			} catch (NoSuchFieldException e) {
				current = current.getSuperclass();
			}
		}
		throw new NoSuchFieldException("id");
	}

	private void setUpdatedAt(Object entity, LocalDateTime updatedAt) throws Exception {
		Field field = entity.getClass().getSuperclass().getDeclaredField("updatedAt");
		field.setAccessible(true);
		field.set(entity, updatedAt);
	}
}
