package com.teamsync.back.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.CircularDependencyException;
import com.teamsync.back.common.exception.CrossProjectDependencyException;
import com.teamsync.back.common.exception.DuplicateDependencyException;
import com.teamsync.back.common.exception.SelfDependencyException;
import com.teamsync.back.common.exception.TaskDependencyNotFoundException;
import com.teamsync.back.common.exception.TaskNotFoundException;
import com.teamsync.back.project.Project;
import com.teamsync.back.task.dto.TaskDependencyCreateRequest;
import com.teamsync.back.task.dto.TaskDependencyResponse;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.workspace.Workspace;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * FR-107(P2, 축소 범위) TaskDependencyService 단위 테스트.
 * 검증 순서(존재 확인 -> self -> cross-project -> duplicate -> circular)와 순환 감지 BFS를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class TaskDependencyServiceTest {

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private TaskDependencyRepository taskDependencyRepository;

	@Mock
	private UserRepository userRepository;

	private TaskDependencyService taskDependencyService;
	private Workspace workspace;
	private Project projectAlpha;
	private Project projectBeta;
	private AuthenticatedUser principal;

	@BeforeEach
	void setUp() throws Exception {
		taskDependencyService = new TaskDependencyService(taskRepository, taskDependencyRepository, userRepository);
		workspace = new Workspace("그로우테크", "growtech.io");
		setId(workspace, 10L);
		projectAlpha = new Project(workspace, "알파", "설명", null);
		setId(projectAlpha, 100L);
		projectBeta = new Project(workspace, "베타", "설명", null);
		setId(projectBeta, 200L);
		principal = new AuthenticatedUser(1L, 10L, "member@growtech.io", Role.MEMBER);
	}

	@Test
	void 같은_프로젝트_태스크간_정상_생성된다() throws Exception {
		Task predecessor = newTask(projectAlpha, 1L);
		Task successor = newTask(projectAlpha, 2L);
		mockTaskLookup(predecessor, successor);
		when(taskDependencyRepository.existsByPredecessorTask_IdAndSuccessorTask_Id(1L, 2L)).thenReturn(false);
		when(taskDependencyRepository.findSuccessorTaskIdsByPredecessorTaskId(2L)).thenReturn(List.of());
		User creator = new User(workspace, "member@growtech.io", "pw", "멤버", Role.MEMBER);
		when(userRepository.getReferenceById(1L)).thenReturn(creator);
		when(taskDependencyRepository.save(any(TaskDependency.class))).thenAnswer(invocation -> {
			TaskDependency dependency = invocation.getArgument(0);
			setId(dependency, 999L);
			return dependency;
		});

		TaskDependencyResponse response = taskDependencyService.createDependency(principal, 2L,
				new TaskDependencyCreateRequest(1L));

		assertThat(response.dependencyId()).isEqualTo(999L);
		assertThat(response.predecessorTask().id()).isEqualTo(1L);
		assertThat(response.successorTask().id()).isEqualTo(2L);
	}

	@Test
	void taskId나_predecessorTaskId가_존재하지_않으면_TASK_NOT_FOUND() {
		when(taskRepository.findByIdAndProject_Workspace_Id(2L, 10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> taskDependencyService.createDependency(principal, 2L,
				new TaskDependencyCreateRequest(1L)))
				.isInstanceOf(TaskNotFoundException.class);
	}

	@Test
	void 자기_자신을_선행_태스크로_지정하면_SELF_DEPENDENCY() throws Exception {
		Task task = newTask(projectAlpha, 5L);
		when(taskRepository.findByIdAndProject_Workspace_Id(5L, 10L)).thenReturn(Optional.of(task));

		assertThatThrownBy(() -> taskDependencyService.createDependency(principal, 5L,
				new TaskDependencyCreateRequest(5L)))
				.isInstanceOf(SelfDependencyException.class);
	}

	@Test
	void 서로_다른_프로젝트_태스크간_생성시_CROSS_PROJECT_DEPENDENCY() throws Exception {
		Task predecessor = newTask(projectBeta, 1L);
		Task successor = newTask(projectAlpha, 2L);
		mockTaskLookup(predecessor, successor);

		assertThatThrownBy(() -> taskDependencyService.createDependency(principal, 2L,
				new TaskDependencyCreateRequest(1L)))
				.isInstanceOf(CrossProjectDependencyException.class);
	}

	@Test
	void 이미_존재하는_조합이면_DUPLICATE_DEPENDENCY() throws Exception {
		Task predecessor = newTask(projectAlpha, 1L);
		Task successor = newTask(projectAlpha, 2L);
		mockTaskLookup(predecessor, successor);
		when(taskDependencyRepository.existsByPredecessorTask_IdAndSuccessorTask_Id(1L, 2L)).thenReturn(true);

		assertThatThrownBy(() -> taskDependencyService.createDependency(principal, 2L,
				new TaskDependencyCreateRequest(1L)))
				.isInstanceOf(DuplicateDependencyException.class);
	}

	@Test
	void 직접_순환이_생기면_CIRCULAR_DEPENDENCY() throws Exception {
		// 기존: A(predecessor) -> B(successor). 이제 taskId=A(2번째 인자 successor 자리), predecessorTaskId=B로
		// "B -> A" 관계를 추가하려 하면 A -> B -> A로 되돌아오는 순환이 생긴다.
		Task taskA = newTask(projectAlpha, 1L);
		Task taskB = newTask(projectAlpha, 2L);
		// createDependency(principal, taskId=1(A), predecessorTaskId=2(B))
		mockTaskLookupFor(1L, taskA);
		mockTaskLookupFor(2L, taskB);
		when(taskDependencyRepository.existsByPredecessorTask_IdAndSuccessorTask_Id(2L, 1L)).thenReturn(false);
		// A(taskId=1)에서 시작한 BFS: A의 successor가 B(기존 A->B 엣지) -> target(predecessorTaskId=2, 즉 B)에 바로 도달
		when(taskDependencyRepository.findSuccessorTaskIdsByPredecessorTaskId(1L)).thenReturn(List.of(2L));

		assertThatThrownBy(() -> taskDependencyService.createDependency(principal, 1L,
				new TaskDependencyCreateRequest(2L)))
				.isInstanceOf(CircularDependencyException.class);
	}

	@Test
	void 여러_홉을_거친_간접_순환도_감지한다() throws Exception {
		// 기존: A -> X -> B. taskId=A, predecessorTaskId=B로 "B -> A"를 추가하려 하면
		// A -> X -> B -> A 순환이 생긴다.
		Task taskA = newTask(projectAlpha, 1L);
		Task taskB = newTask(projectAlpha, 3L);
		mockTaskLookupFor(1L, taskA);
		mockTaskLookupFor(3L, taskB);
		when(taskDependencyRepository.existsByPredecessorTask_IdAndSuccessorTask_Id(3L, 1L)).thenReturn(false);
		when(taskDependencyRepository.findSuccessorTaskIdsByPredecessorTaskId(1L)).thenReturn(List.of(2L)); // A -> X
		when(taskDependencyRepository.findSuccessorTaskIdsByPredecessorTaskId(2L)).thenReturn(List.of(3L)); // X -> B

		assertThatThrownBy(() -> taskDependencyService.createDependency(principal, 1L,
				new TaskDependencyCreateRequest(3L)))
				.isInstanceOf(CircularDependencyException.class);
	}

	@Test
	void 순환이_아닌_중복되지_않은_정방향_경로는_정상_생성된다() throws Exception {
		// 기존: B -> X (predecessor=B, successor=X). 이제 taskId=X, predecessorTaskId=B로
		// "B -> X" 직접 관계를 추가하는 상황(다른 predecessor B, 다른 타겟)이 아니라, 순환이 없는 케이스를 검증한다.
		// A(taskId) 입장에서 predecessorTaskId=B를 추가할 때 A에서 B로 가는 기존 경로가 전혀 없으면 정상 생성되어야 한다.
		Task taskA = newTask(projectAlpha, 1L);
		Task taskB = newTask(projectAlpha, 2L);
		mockTaskLookupFor(1L, taskA);
		mockTaskLookupFor(2L, taskB);
		when(taskDependencyRepository.existsByPredecessorTask_IdAndSuccessorTask_Id(2L, 1L)).thenReturn(false);
		when(taskDependencyRepository.findSuccessorTaskIdsByPredecessorTaskId(1L)).thenReturn(List.of());
		User creator = new User(workspace, "member@growtech.io", "pw", "멤버", Role.MEMBER);
		when(userRepository.getReferenceById(1L)).thenReturn(creator);
		when(taskDependencyRepository.save(any(TaskDependency.class))).thenAnswer(invocation -> {
			TaskDependency dependency = invocation.getArgument(0);
			setId(dependency, 1000L);
			return dependency;
		});

		TaskDependencyResponse response = taskDependencyService.createDependency(principal, 1L,
				new TaskDependencyCreateRequest(2L));

		assertThat(response.dependencyId()).isEqualTo(1000L);
	}

	@Test
	void 삭제시_dependencyId가_taskId와_무관하면_TASK_DEPENDENCY_NOT_FOUND() throws Exception {
		Task task = newTask(projectAlpha, 5L);
		when(taskRepository.findByIdAndProject_Workspace_Id(5L, 10L)).thenReturn(Optional.of(task));
		when(taskDependencyRepository.findByIdAndPredecessorTask_Id(50L, 5L)).thenReturn(Optional.empty());
		when(taskDependencyRepository.findByIdAndSuccessorTask_Id(50L, 5L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> taskDependencyService.deleteDependency(principal, 5L, 50L))
				.isInstanceOf(TaskDependencyNotFoundException.class);
		verify(taskDependencyRepository, never()).delete(any());
	}

	private void mockTaskLookup(Task predecessor, Task successor) {
		mockTaskLookupFor(1L, predecessor);
		mockTaskLookupFor(2L, successor);
	}

	private void mockTaskLookupFor(Long id, Task task) {
		lenient().when(taskRepository.findByIdAndProject_Workspace_Id(id, 10L)).thenReturn(Optional.of(task));
	}

	private Task newTask(Project project, Long id) throws Exception {
		Task task = new Task(project, "태스크" + id, null, TaskPriority.MEDIUM, TaskStatus.TODO, null, null, null,
				Collections.emptySet());
		setId(task, id);
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
