package com.teamsync.back.task.issue;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskIssueFlagRepository extends JpaRepository<TaskIssueFlag, Long> {

	// FR-406 배치 전용: 프로젝트의 현재 OPEN 플래그 전체를 (taskId, kind) 기준으로 판단하기 위한 조회.
	// task는 lazy 프록시로도 getId()/getKind() 판단에는 충분하므로 EntityGraph 없이 가볍게 가져온다.
	List<TaskIssueFlag> findAllByTask_Project_IdAndStatus(Long projectId, TaskIssueStatus status);

	// GET /api/projects/{projectId}/issues 응답 조립용: taskTitle/assignees/resolvedBy를
	// N+1 없이 함께 로딩한다.
	@EntityGraph(attributePaths = {"task", "task.assignees", "resolvedBy"})
	List<TaskIssueFlag> findAllByTask_Project_IdAndStatusOrderByDetectedAtDesc(Long projectId, TaskIssueStatus status);

	@EntityGraph(attributePaths = {"task", "task.assignees", "resolvedBy"})
	List<TaskIssueFlag> findAllByTask_Project_IdAndStatusAndKindOrderByDetectedAtDesc(Long projectId,
			TaskIssueStatus status, TaskIssueKind kind);

	// PATCH .../issues/{issueId}/resolve: issueId가 해당 프로젝트 소속인지 검증(아니면 404)하면서
	// 응답 조립에 필요한 연관관계도 함께 로딩한다.
	@EntityGraph(attributePaths = {"task", "task.assignees", "resolvedBy"})
	Optional<TaskIssueFlag> findByIdAndTask_Project_Id(Long id, Long projectId);
}
