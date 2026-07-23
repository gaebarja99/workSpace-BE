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
}
