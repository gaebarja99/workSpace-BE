package com.teamsync.back.task.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.common.exception.TaskIssueFlagNotFoundException;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.TaskStatus;
import com.teamsync.back.task.issue.dto.TaskIssueItemResponse;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.workspace.Workspace;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * FR-406 API 서비스(TaskIssueFlagService) 단위 테스트. resolve의 멱등성(이미 RESOLVED면
 * 에러 없이 현재 상태 반환)과 프로젝트/이슈 워크스페이스 스코핑(404)을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class TaskIssueFlagServiceTest {

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private TaskIssueFlagRepository taskIssueFlagRepository;

	@Mock
	private UserRepository userRepository;

	private TaskIssueFlagService service;
	private Workspace workspace;
	private Project project;
	private AuthenticatedUser leaderPrincipal;

	@BeforeEach
	void setUp() throws Exception {
		service = new TaskIssueFlagService(projectRepository, taskIssueFlagRepository, userRepository);
		workspace = new Workspace("그로우테크", "growtech.io");
		setId(workspace, 10L);
		project = new Project(workspace, "알파", "설명", null);
		setId(project, 100L);
		leaderPrincipal = new AuthenticatedUser(1L, 10L, "leader@growtech.io", Role.LEADER);
	}

	@Test
	void 프로젝트가_다른_워크스페이스_소속이면_PROJECT_NOT_FOUND() {
		when(projectRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.resolveIssue(leaderPrincipal, 100L, 1L))
				.isInstanceOf(ProjectNotFoundException.class);
	}

	@Test
	void issueId가_해당_프로젝트_소속이_아니면_TASK_ISSUE_NOT_FOUND() {
		when(projectRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(project));
		when(taskIssueFlagRepository.findByIdAndTask_Project_Id(1L, 100L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.resolveIssue(leaderPrincipal, 100L, 1L))
				.isInstanceOf(TaskIssueFlagNotFoundException.class);
	}

	@Test
	void OPEN_이슈를_resolve하면_RESOLVED로_전환되고_resolvedBy가_설정된다() throws Exception {
		Task task = newTask();
		TaskIssueFlag flag = new TaskIssueFlag(task, TaskIssueKind.OVERDUE, "3일 초과", LocalDateTime.now());
		setId(flag, 1L);
		User resolvedByUser = new User(workspace, "leader@growtech.io", "pw", "팀장", Role.LEADER);
		when(projectRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(project));
		when(taskIssueFlagRepository.findByIdAndTask_Project_Id(1L, 100L)).thenReturn(Optional.of(flag));
		when(userRepository.getReferenceById(1L)).thenReturn(resolvedByUser);

		TaskIssueItemResponse response = service.resolveIssue(leaderPrincipal, 100L, 1L);

		assertThat(response.status()).isEqualTo(TaskIssueStatus.RESOLVED);
		assertThat(response.resolvedBy()).isEqualTo("팀장");
	}

	@Test
	void 이미_RESOLVED인_이슈를_다시_resolve해도_에러_없이_현재_상태를_반환한다() throws Exception {
		Task task = newTask();
		TaskIssueFlag flag = new TaskIssueFlag(task, TaskIssueKind.OVERDUE, "3일 초과", LocalDateTime.now());
		setId(flag, 1L);
		flag.resolve(null); // 이미 시스템에 의해 RESOLVED된 상태.
		when(projectRepository.findByIdAndWorkspaceId(100L, 10L)).thenReturn(Optional.of(project));
		when(taskIssueFlagRepository.findByIdAndTask_Project_Id(1L, 100L)).thenReturn(Optional.of(flag));

		TaskIssueItemResponse response = service.resolveIssue(leaderPrincipal, 100L, 1L);

		assertThat(response.status()).isEqualTo(TaskIssueStatus.RESOLVED);
		assertThat(response.resolvedBy()).isNull();
		verify(userRepository, never()).getReferenceById(org.mockito.ArgumentMatchers.anyLong());
	}

	private Task newTask() throws Exception {
		Task task = new Task(project, "태스크", "설명", TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, null, null, null,
				Set.of());
		setId(task, 5L);
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
}
