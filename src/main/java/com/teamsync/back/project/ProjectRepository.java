package com.teamsync.back.project;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

	List<Project> findAllByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

	// GET /api/projects: 로그인 사용자가 실제로 참여 중인(project_members) 프로젝트만 반환한다.
	// (관리자 전용 GET /api/admin/projects는 워크스페이스 전체 프로젝트를 그대로 보여줘야 하므로 이 쿼리를 쓰지 않는다.)
	@Query("SELECT p FROM Project p JOIN ProjectMember pm ON pm.project = p "
			+ "WHERE p.workspace.id = :workspaceId AND pm.user.id = :userId ORDER BY p.createdAt DESC")
	List<Project> findAllByWorkspaceIdAndMemberUserId(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId);

	// 태스크 도메인(FR-101/102)에서 프로젝트가 요청자의 워크스페이스 소속인지 스코핑 검증할 때 사용.
	// 다른 워크스페이스 프로젝트는 조회되지 않아 존재 자체가 노출되지 않는다(PRD 5.6 리스크 대응).
	Optional<Project> findByIdAndWorkspaceId(Long id, Long workspaceId);

	// 프로젝트 관리(관리자, P2): GET /api/admin/projects/stats의 total 카운트에 사용.
	long countByWorkspaceId(Long workspaceId);

	// 프로젝트 관리(관리자, P2): GET /api/admin/projects/stats에서 status별 카운트 집계에 사용.
	long countByWorkspaceIdAndStatus(Long workspaceId, ProjectStatus status);

	// FR-407(조직 롤업 대시보드): "팀"=Project 중 ACTIVE(진행중)만 대상으로 하며, projectId 오름차순으로
	// 응답한다(계약 문서 명시).
	List<Project> findAllByWorkspaceIdAndStatusOrderByIdAsc(Long workspaceId, ProjectStatus status);

	// FR-004(통합 검색): name에 키워드가 포함된 프로젝트를 워크스페이스 범위로 조회한다.
	// keyword는 호출부에서 LIKE 와일드카드(%, _)를 이스케이프해 넘겨야 한다(ESCAPE '\' 사용).
	@Query("SELECT p FROM Project p WHERE p.workspace.id = :workspaceId "
			+ "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' "
			+ "ORDER BY p.createdAt DESC, p.id DESC")
	List<Project> searchByWorkspace(@Param("workspaceId") Long workspaceId, @Param("keyword") String keyword,
			Pageable pageable);
}
