package com.teamsync.back.project.dto;

import jakarta.validation.constraints.NotNull;

/** POST /api/projects/{projectId}/members 요청 본문. */
public record AddMemberRequest(
		@NotNull Long userId
) {
}
