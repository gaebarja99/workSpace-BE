package com.teamsync.back.project;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

	List<Project> findAllByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
}
