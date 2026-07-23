package com.teamsync.back.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	// 태스크 담당자 지정(FR-102) 시 다른 워크스페이스 사용자가 섞여 들어오지 않도록 스코핑 검증.
	List<User> findAllByIdInAndWorkspaceId(Collection<Long> ids, Long workspaceId);

	// FR-301 담당자 선택용 프로젝트 멤버 목록(GET /api/projects/{projectId}/members): 프로젝트별
	// 멤버십 테이블이 없으므로 "프로젝트 멤버" = "해당 워크스페이스 전체 User"로 조회한다.
	List<User> findAllByWorkspaceIdOrderByNameAsc(Long workspaceId);
}
