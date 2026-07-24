package com.teamsync.back.project.dto;

/** 프로젝트 관리(관리자, P2): GET /api/admin/projects/stats 응답. */
public record ProjectStatsResponse(
		long total,
		long active,
		long planned,
		long archived
) {
}
