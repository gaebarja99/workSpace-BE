package com.teamsync.back.project.dto;

import com.teamsync.back.project.Project;
import java.time.LocalDateTime;

public record ProjectResponse(
		Long id,
		String name,
		String description,
		Long workspaceId,
		Long createdById,
		LocalDateTime createdAt
) {
	public static ProjectResponse from(Project project) {
		return new ProjectResponse(
				project.getId(),
				project.getName(),
				project.getDescription(),
				project.getWorkspace().getId(),
				project.getCreatedBy() != null ? project.getCreatedBy().getId() : null,
				project.getCreatedAt());
	}
}
