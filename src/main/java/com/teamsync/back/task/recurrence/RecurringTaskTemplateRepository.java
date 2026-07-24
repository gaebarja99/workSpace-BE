package com.teamsync.back.task.recurrence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTaskTemplateRepository extends JpaRepository<RecurringTaskTemplate, Long> {

	@EntityGraph(attributePaths = {"assignees", "createdBy"})
	List<RecurringTaskTemplate> findAllByProject_IdOrderByIdAsc(Long projectId);

	@EntityGraph(attributePaths = {"assignees", "createdBy"})
	Optional<RecurringTaskTemplate> findByIdAndProject_Workspace_Id(Long id, Long workspaceId);

	// FR-106 배치 전용: active=true인 템플릿 전체(시스템 전역)를 대상으로 하며, Task 생성에 필요한
	// project/createdBy/assignees를 한 번에 즉시 로딩해 N+1을 피한다.
	@EntityGraph(attributePaths = {"assignees", "createdBy", "project"})
	List<RecurringTaskTemplate> findAllByActiveTrue();
}
