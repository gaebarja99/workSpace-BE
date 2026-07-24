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

	// FR-106 배치 전용: active=true인 템플릿 전체(시스템 전역)를 대상으로 하며, 생성 여부 판단
	// (shouldGenerateToday)에 필요한 기본 컬럼만 조회한다. project/assignees/createdBy는 여기서
	// 즉시 로딩하지 않는다 — 이 조회는 트랜잭션 밖(RecurringTaskSchedulerService)에서 실행되므로
	// 반환된 엔티티는 곧 detached 상태가 되고, LAZY 연관관계는 이후 findWithDetailsById로 템플릿별
	// 독립 트랜잭션 안에서 다시 조회해 사용한다.
	List<RecurringTaskTemplate> findAllByActiveTrue();

	// FR-106 배치의 템플릿별 Task 생성(RecurringTaskGenerationService, REQUIRES_NEW 트랜잭션)
	// 전용: project/createdBy/assignees를 한 번에 즉시 로딩해 N+1을 피한다.
	@EntityGraph(attributePaths = {"assignees", "createdBy", "project"})
	Optional<RecurringTaskTemplate> findWithDetailsById(Long id);
}
