package com.teamsync.back.task;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {

	// FR-105-B: GET /api/tasks/{taskId}/activities — createdAt 오름차순(동일 시각은 id 오름차순으로 안정화).
	// actorName 조립을 위해 actor를 함께 즉시 로딩한다(N+1 방지).
	@EntityGraph(attributePaths = "actor")
	List<TaskActivity> findByTaskIdOrderByCreatedAtAscIdAsc(Long taskId);
}
