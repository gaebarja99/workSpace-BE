package com.teamsync.back.project.dto;

import com.teamsync.back.project.Project;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 프로젝트 관리(관리자, P2): GET /api/admin/projects 응답.
 * memberCount는 project_members 테이블(V26)의 실제 등록 인원 수다.
 */
public record ProjectAdminResponse(
		Long id,
		String name,
		String description,
		LocalDate deadline,
		String status,
		CreatedBy createdBy,
		long memberCount,
		LocalDateTime createdAt
) {
	public static ProjectAdminResponse of(Project project, long memberCount) {
		CreatedBy createdBy = project.getCreatedBy() != null
				? new CreatedBy(project.getCreatedBy().getId(), project.getCreatedBy().getName())
				: null;
		return new ProjectAdminResponse(
				project.getId(),
				project.getName(),
				project.getDescription(),
				project.getDeadline(),
				project.getStatus().name(),
				createdBy,
				memberCount,
				project.getCreatedAt());
	}

	public record CreatedBy(Long id, String name) {
	}
}
