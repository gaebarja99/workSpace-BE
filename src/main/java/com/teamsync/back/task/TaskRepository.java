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
}
