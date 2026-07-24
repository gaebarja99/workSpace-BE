package com.teamsync.back.project;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

	List<Project> findAllByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

	// 태스크 도메인(FR-101/102)에서 프로젝트가 요청자의 워크스페이스 소속인지 스코핑 검증할 때 사용.
	// 다른 워크스페이스 프로젝트는 조회되지 않아 존재 자체가 노출되지 않는다(PRD 5.6 리스크 대응).
	Optional<Project> findByIdAndWorkspaceId(Long id, Long workspaceId);

	// 프로젝트 관리(관리자, P2): GET /api/admin/projects/stats의 total 카운트에 사용.
	long countByWorkspaceId(Long workspaceId);

	// 프로젝트 관리(관리자, P2): GET /api/admin/projects/stats에서 status별 카운트 집계에 사용.
	long countByWorkspaceIdAndStatus(Long workspaceId, ProjectStatus status);
}
