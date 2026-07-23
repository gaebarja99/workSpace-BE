package com.teamsync.back.task;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskChecklistItemRepository extends JpaRepository<TaskChecklistItem, Long> {

	long countByTaskId(Long taskId);

	Optional<TaskChecklistItem> findByIdAndTaskId(Long id, Long taskId);
}
