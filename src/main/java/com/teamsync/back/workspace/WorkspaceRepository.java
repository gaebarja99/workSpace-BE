package com.teamsync.back.workspace;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

	Optional<Workspace> findByDomain(String domain);

	boolean existsByDomain(String domain);
}
