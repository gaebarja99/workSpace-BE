package com.teamsync.back.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

	@EntityGraph(attributePaths = "assignees")
	List<Task> findAllByProjectIdOrderByDueDateAscIdAsc(Long projectId);

	// 주의: assignees(ManyToMany)와 checklistItems(OneToMany)를 하나의 EntityGraph로 동시에
	// JOIN FETCH하면 카티전 곱으로 인해 checklistItems가 중복 행으로 반환되는 버그가 있었다.
	// 따라서 이 조회는 assignees만 즉시 로딩하고, checklistItems는 트랜잭션 내에서 지연 로딩되도록 둔다
	// (상세 조회 1건에 한해 컬렉션당 추가 SELECT 1회는 허용 가능한 비용).
	@EntityGraph(attributePaths = "assignees")
	Optional<Task> findByIdAndProject_Workspace_Id(Long id, Long workspaceId);

	// FR-104(담당자별 대시보드, US-01 "내 업무"): 현재 사용자가 담당자인, 워크스페이스 소속이면서
	// 완료되지 않은 태스크만 조회한다. assignees(ManyToMany)와 project(ManyToOne)는 서로 다른
	// 연관관계 타입(1:N이 아닌 N:1)이라 checklistItems 때와 달리 카티전 곱 중복 문제가 없어
	// 하나의 EntityGraph로 함께 즉시 로딩해도 안전하다(project.name 접근 시 지연 로딩 예외 방지 목적).
	// dueDate ASC(값이 없으면 마지막)/id ASC까지만 쿼리로 처리하고, priority 2차 정렬은
	// 서비스 레이어에서 Comparator로 최종 확정한다.
	@EntityGraph(attributePaths = {"assignees", "project"})
	List<Task> findAllByAssignees_IdAndProject_Workspace_IdAndStatusNotOrderByDueDateAscIdAsc(
			Long assigneeId, Long workspaceId, TaskStatus excludedStatus);

	// FR-108(마감 임박 배치, US-04): dueDate가 오늘/내일이고 완료(DONE)되지 않은 태스크를 워크스페이스
	// 전체(배치는 시스템 전역에서 1일 1회 실행)에서 조회한다. 담당자 순회를 위해 assignees를 함께 로딩한다.
	@EntityGraph(attributePaths = "assignees")
	List<Task> findAllByDueDateAndStatusNot(LocalDate dueDate, TaskStatus excludedStatus);

	// FR-004(통합 검색): title 또는 description에 키워드가 포함된 태스크를 워크스페이스 범위로 조회한다.
	// title/description OR 조건에 대소문자 무시 파생 쿼리명을 쓰면 지나치게 길어져(project.workspace.id를
	// 두 번 반복) @Query(JPQL)로 작성한다. project를 JOIN FETCH해 응답의 projectName 조립 시 N+1을 피한다.
	// keyword는 호출부에서 LIKE 와일드카드(%, _)를 이스케이프해 넘겨야 한다(ESCAPE '\' 사용).
	@Query("SELECT t FROM Task t JOIN FETCH t.project p "
			+ "WHERE p.workspace.id = :workspaceId "
			+ "AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' "
			+ "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\') "
			+ "ORDER BY t.createdAt DESC, t.id DESC")
	List<Task> searchByWorkspace(@Param("workspaceId") Long workspaceId, @Param("keyword") String keyword,
			Pageable pageable);

	// FR-401(주간 보고 자동 취합 "완료한 일"): project+담당자 기준, status=DONE AND updatedAt이
	// [weekStart, weekEnd+1day) 범위인 태스크(계약 문서 규칙 그대로). 스냅샷 없이 매번 실시간 계산한다.
	@EntityGraph(attributePaths = "assignees")
	List<Task> findAllByProject_IdAndAssignees_IdAndStatusAndUpdatedAtBetween(
			Long projectId, Long assigneeId, TaskStatus status, LocalDateTime updatedAtStart,
			LocalDateTime updatedAtEnd);

	// FR-401(주간 보고 자동 취합 "진행 중인 일"/이슈): project+담당자 기준, 완료되지 않은 태스크 전체.
	// "진행 중" isNew(createdAt 범위) 여부와 "이슈"(OVERDUE/STALE) 판정은 이 목록으로 서비스 계층에서
	// 함께 계산해 중복 조회를 피한다.
	@EntityGraph(attributePaths = "assignees")
	List<Task> findAllByProject_IdAndAssignees_IdAndStatusNot(Long projectId, Long assigneeId,
			TaskStatus excludedStatus);

	// 프로젝트 관리(관리자, P2) DELETE 사전 검증: 프로젝트에 태스크가 하나라도 남아있는지 확인한다.
	boolean existsByProject_Id(Long projectId);

	// FR-407(조직 롤업 대시보드) "완료 태스크 수": 위 findAllByProject_IdAndAssignees_IdAndStatusAndUpdatedAtBetween와
	// 동일한 FR-401 OVERDUE 판정 윈도우 규칙(status=DONE AND updatedAt in [weekStart, weekEnd+1일))을 담당자
	// 구분 없이 프로젝트 전체로 확장한 버전이다(한 태스크에 담당자가 여러 명이어도 1건으로만 집계하기 위해
	// assignees 필터를 제거).
	List<Task> findAllByProject_IdAndStatusAndUpdatedAtBetween(
			Long projectId, TaskStatus status, LocalDateTime updatedAtStart, LocalDateTime updatedAtEnd);

	// FR-407(조직 롤업 대시보드) "진행 중+이슈 판정 대상": 위 findAllByProject_IdAndAssignees_IdAndStatusNot와
	// 동일하되 담당자 구분 없이 프로젝트 전체 미완료 태스크를 조회한다.
	List<Task> findAllByProject_IdAndStatusNot(Long projectId, TaskStatus excludedStatus);

	// FR-406(이슈/리스크 자동 플래그) 배치 전용: 프로젝트 내 미완료 태스크 전체 + 담당자를 함께 로딩해
	// (알림 발송 대상 조립 시 N+1을 피한다) id 오름차순 결정적 순서로 순회한다.
	@EntityGraph(attributePaths = "assignees")
	List<Task> findAllByProject_IdAndStatusNotOrderByIdAsc(Long projectId, TaskStatus excludedStatus);
}
