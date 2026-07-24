package com.teamsync.back.task;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

	// "successors" 목록 조회(응답 조립) 전용. successorTask.assignees까지 함께 로딩해 TaskSummaryResponse
	// 변환 시 N+1을 피한다(단일 컬렉션만 JOIN FETCH하므로 카티전 곱 문제 없음).
	@EntityGraph(attributePaths = {"successorTask", "successorTask.assignees"})
	List<TaskDependency> findByPredecessorTask_Id(Long predecessorTaskId);

	// 순환 감지(BFS) 전용 경량 조회: 엔티티 전체가 아닌 successorTask의 id만 필요하므로 프로젝션으로
	// 최소한의 컬럼만 가져온다(그래프 탐색 중 매 홉마다 호출되므로 assignees 등 불필요한 로딩을 피한다).
	@Query("SELECT d.successorTask.id FROM TaskDependency d WHERE d.predecessorTask.id = :predecessorTaskId")
	List<Long> findSuccessorTaskIdsByPredecessorTaskId(@Param("predecessorTaskId") Long predecessorTaskId);

	// "predecessors" 목록 조회(응답 조립) 전용. predecessorTask.assignees까지 함께 로딩한다.
	@EntityGraph(attributePaths = {"predecessorTask", "predecessorTask.assignees"})
	List<TaskDependency> findBySuccessorTask_Id(Long successorTaskId);

	boolean existsByPredecessorTask_IdAndSuccessorTask_Id(Long predecessorTaskId, Long successorTaskId);

	Optional<TaskDependency> findByIdAndPredecessorTask_Id(Long id, Long predecessorTaskId);

	Optional<TaskDependency> findByIdAndSuccessorTask_Id(Long id, Long successorTaskId);
}
