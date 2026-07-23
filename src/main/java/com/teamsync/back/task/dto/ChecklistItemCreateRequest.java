package com.teamsync.back.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChecklistItemCreateRequest(
		@NotBlank(message = "체크리스트 항목 내용은 필수입니다.")
		@Size(max = 500)
		String content
) {
}
