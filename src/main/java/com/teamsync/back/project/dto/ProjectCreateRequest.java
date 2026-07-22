package com.teamsync.back.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
		@NotBlank(message = "프로젝트 이름은 필수입니다.")
		@Size(max = 200)
		String name,

		String description
) {
}
