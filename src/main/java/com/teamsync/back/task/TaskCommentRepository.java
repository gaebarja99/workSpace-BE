package com.teamsync.back.task;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

	@EntityGraph(attributePaths = "author")
	List<TaskComment> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}
