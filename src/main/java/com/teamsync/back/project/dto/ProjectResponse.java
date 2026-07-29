package com.teamsync.back.project.dto;

import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponse(
		Long id,
		String name,
		String description,
		LocalDate deadline,
		Long workspaceId,
		Long createdById,
		LocalDateTime createdAt,
		ProjectStatus status
) {
	public static ProjectResponse from(Project project) {
		return new ProjectResponse(
				project.getId(),
				project.getName(),
				project.getDescription(),
				project.getDeadline(),
				project.getWorkspace().getId(),
				project.getCreatedBy() != null ? project.getCreatedBy().getId() : null,
				project.getCreatedAt(),
				project.getStatus());
	}
}
