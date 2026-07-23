package com.teamsync.back.task;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
