package com.teamsync.back.project;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

	// GET /api/projects/{projectId}/members: 실제 프로젝트 멤버만 이름순으로 조회한다.
	List<ProjectMember> findAllByProject_IdOrderByUser_NameAsc(Long projectId);

	boolean existsByProject_IdAndUser_Id(Long projectId, Long userId);

	Optional<ProjectMember> findByProject_IdAndUser_Id(Long projectId, Long userId);

	// GET /api/admin/projects의 memberCount, "마지막 멤버는 제거 불가" 검증에 사용.
	long countByProject_Id(Long projectId);

	// GET /api/projects/{projectId}/members/candidates: 이미 멤버인 사용자를 제외하기 위한 id 목록 조회.
	@Query("SELECT pm.user.id FROM ProjectMember pm WHERE pm.project.id = :projectId")
	List<Long> findUserIdsByProject_Id(@Param("projectId") Long projectId);

	// DELETE /api/admin/projects/{id}: project_members는 ON DELETE 정책이 없어(V26) 프로젝트
	// 삭제 전에 먼저 정리해야 한다.
	void deleteByProject_Id(Long projectId);
}
