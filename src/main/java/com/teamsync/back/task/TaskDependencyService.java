package com.teamsync.back.task;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.CircularDependencyException;
import com.teamsync.back.common.exception.CrossProjectDependencyException;
import com.teamsync.back.common.exception.DuplicateDependencyException;
import com.teamsync.back.common.exception.SelfDependencyException;
import com.teamsync.back.common.exception.TaskDependencyNotFoundException;
import com.teamsync.back.common.exception.TaskNotFoundException;
import com.teamsync.back.task.dto.TaskDependencyCreateRequest;
import com.teamsync.back.task.dto.TaskDependencyItemResponse;
import com.teamsync.back.task.dto.TaskDependencyListResponse;
import com.teamsync.back.task.dto.TaskDependencyResponse;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-107(P2, 축소 범위): 태스크 간 선후행 의존관계 CRUD + 순환 감지. 간트뷰/타임라인 시각화나
 * 의존관계에 따른 태스크 상태 전환 제한 등의 비즈니스 규칙은 이번 범위 밖이다.
 * TaskService와 동일한 원칙으로 taskId/predecessorTaskId가 요청자의 워크스페이스에 실제로
 * 속하는지 항상 principal.workspaceId() 기준으로 재검증한다(PRD 5.6 리스크 대응).
 */
@Service
public class TaskDependencyService {

	private final TaskRepository taskRepository;
	private final TaskDependencyRepository taskDependencyRepository;
	private final UserRepository userRepository;

	public TaskDependencyService(TaskRepository taskRepository, TaskDependencyRepository taskDependencyRepository,
			UserRepository userRepository) {
		this.taskRepository = taskRepository;
		this.taskDependencyRepository = taskDependencyRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public TaskDependencyListResponse listDependencies(AuthenticatedUser principal, Long taskId) {
		getTaskInWorkspace(principal, taskId);

		List<TaskDependencyItemResponse> predecessors = taskDependencyRepository.findBySuccessorTask_Id(taskId)
				.stream()
				.map(TaskDependencyItemResponse::ofPredecessor)
				.toList();
		List<TaskDependencyItemResponse> successors = taskDependencyRepository.findByPredecessorTask_Id(taskId)
				.stream()
				.map(TaskDependencyItemResponse::ofSuccessor)
				.toList();
		return new TaskDependencyListResponse(predecessors, successors);
	}

	/**
	 * 검증 순서(계약 문서 고정): (1) taskId/predecessorTaskId 존재 확인(404 TASK_NOT_FOUND) →
	 * (2) 자기참조(400 SELF_DEPENDENCY) → (3) 프로젝트 불일치(400 CROSS_PROJECT_DEPENDENCY) →
	 * (4) 중복 조합(409 DUPLICATE_DEPENDENCY) → (5) 순환 발생(409 CIRCULAR_DEPENDENCY).
	 */
	@Transactional
	public TaskDependencyResponse createDependency(AuthenticatedUser principal, Long taskId,
			TaskDependencyCreateRequest request) {
		Task successorTask = getTaskInWorkspace(principal, taskId);
		Task predecessorTask = getTaskInWorkspace(principal, request.predecessorTaskId());

		if (predecessorTask.getId().equals(successorTask.getId())) {
			throw new SelfDependencyException();
		}
		if (!predecessorTask.getProject().getId().equals(successorTask.getProject().getId())) {
			throw new CrossProjectDependencyException();
		}
		if (taskDependencyRepository.existsByPredecessorTask_IdAndSuccessorTask_Id(predecessorTask.getId(),
				successorTask.getId())) {
			throw new DuplicateDependencyException();
		}
		// taskId(추가될 successor)에서 시작해 이미 predecessorTask.id에 도달할 수 있다면(=predecessorTask가
		// taskId의 후행 태스크 계열에 이미 속해있다면), predecessorTask -> taskId 관계를 추가하는 순간
		// "taskId -> ... -> predecessorTask -> taskId"로 되돌아오는 순환이 만들어진다.
		if (isReachable(taskId, predecessorTask.getId())) {
			throw new CircularDependencyException();
		}

		User createdBy = userRepository.getReferenceById(principal.userId());
		TaskDependency saved = taskDependencyRepository.save(new TaskDependency(predecessorTask, successorTask, createdBy));
		return TaskDependencyResponse.from(saved);
	}

	@Transactional
	public void deleteDependency(AuthenticatedUser principal, Long taskId, Long dependencyId) {
		getTaskInWorkspace(principal, taskId);

		TaskDependency dependency = taskDependencyRepository.findByIdAndPredecessorTask_Id(dependencyId, taskId)
				.or(() -> taskDependencyRepository.findByIdAndSuccessorTask_Id(dependencyId, taskId))
				.orElseThrow(TaskDependencyNotFoundException::new);
		taskDependencyRepository.delete(dependency);
	}

	/**
	 * fromTaskId에서 successor 방향(predecessor -> successor 정방향 엣지)으로 그래프를 BFS 탐색해
	 * targetTaskId에 도달 가능한지 확인한다. 순환 감지에 쓰이는 유일한 그래프 연산이다.
	 */
	private boolean isReachable(Long fromTaskId, Long targetTaskId) {
		Set<Long> visited = new HashSet<>();
		Deque<Long> queue = new ArrayDeque<>();
		visited.add(fromTaskId);
		queue.add(fromTaskId);

		while (!queue.isEmpty()) {
			Long current = queue.poll();
			for (Long successorId : taskDependencyRepository.findSuccessorTaskIdsByPredecessorTaskId(current)) {
				if (successorId.equals(targetTaskId)) {
					return true;
				}
				if (visited.add(successorId)) {
					queue.add(successorId);
				}
			}
		}
		return false;
	}

	private Task getTaskInWorkspace(AuthenticatedUser principal, Long taskId) {
		return taskRepository.findByIdAndProject_Workspace_Id(taskId, principal.workspaceId())
				.orElseThrow(TaskNotFoundException::new);
	}
}
