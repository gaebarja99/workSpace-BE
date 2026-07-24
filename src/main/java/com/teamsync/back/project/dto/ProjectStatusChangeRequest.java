package com.teamsync.back.project.dto;

import com.teamsync.back.project.ProjectStatus;
import jakarta.validation.constraints.NotNull;

/** 프로젝트 관리(관리자, P2): PATCH /api/admin/projects/{id}/status 요청. */
public record ProjectStatusChangeRequest(
		@NotNull(message = "상태는 필수입니다.")
		ProjectStatus status
) {
}
