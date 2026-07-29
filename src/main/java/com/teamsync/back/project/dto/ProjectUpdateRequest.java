package com.teamsync.back.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** 프로젝트 관리(관리자, P2): PATCH /api/admin/projects/{id} 요청(이름/설명/마감일 수정). */
public record ProjectUpdateRequest(
		@NotBlank(message = "프로젝트 이름은 필수입니다.")
		@Size(max = 200)
		String name,

		String description,

		LocalDate deadline
) {
}
