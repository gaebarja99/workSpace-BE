package com.teamsync.back.report.dto;

import jakarta.validation.constraints.NotBlank;

/** PATCH /api/category-keywords/{id} 요청(이름 변경). */
public record CategoryKeywordUpdateRequest(
		@NotBlank String name
) {
}
