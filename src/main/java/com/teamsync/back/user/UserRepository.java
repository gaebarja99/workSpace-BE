package com.teamsync.back.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	// 태스크 담당자 지정(FR-102) 시 다른 워크스페이스 사용자가 섞여 들어오지 않도록 스코핑 검증.
	List<User> findAllByIdInAndWorkspaceId(Collection<Long> ids, Long workspaceId);

	// FR-301 담당자 선택용 프로젝트 멤버 목록(GET /api/projects/{projectId}/members): 프로젝트별
	// 멤버십 테이블이 없으므로 "프로젝트 멤버" = "해당 워크스페이스 전체 User"로 조회한다.
	List<User> findAllByWorkspaceIdOrderByNameAsc(Long workspaceId);

	// FR-206(DM 상대 목록, GET /api/dm/contacts): 워크스페이스 내 전체 사용자 중 호출자 본인만 제외한다.
	List<User> findAllByWorkspaceIdAndIdNotOrderByNameAsc(Long workspaceId, Long excludedUserId);

	// FR-004(통합 검색): name 또는 email에 키워드가 포함된 사용자를 워크스페이스 범위로 조회한다.
	// keyword는 호출부에서 LIKE 와일드카드(%, _)를 이스케이프해 넘겨야 한다(ESCAPE '\' 사용).
	@Query("SELECT u FROM User u WHERE u.workspace.id = :workspaceId "
			+ "AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' "
			+ "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\') "
			+ "ORDER BY u.name ASC, u.id ASC")
	List<User> searchByWorkspace(@Param("workspaceId") Long workspaceId, @Param("keyword") String keyword,
			Pageable pageable);
}
