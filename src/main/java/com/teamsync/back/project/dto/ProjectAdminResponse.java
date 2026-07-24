package com.teamsync.back.project.dto;

import com.teamsync.back.project.Project;
import java.time.LocalDateTime;

/**
 * 프로젝트 관리(관리자, P2): GET /api/admin/projects 응답.
 * memberCount는 프로젝트별 멤버십 테이블이 없어 "프로젝트가 속한 workspace의 전체 User 수"로 근사한다
 * (ProjectService.listMembers와 동일한 근사 방식).
 */
public record ProjectAdminResponse(
		Long id,
		String name,
		String description,
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
				project.getStatus().name(),
				createdBy,
				memberCount,
				project.getCreatedAt());
	}

	public record CreatedBy(Long id, String name) {
	}
}
