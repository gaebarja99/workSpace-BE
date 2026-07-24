package com.teamsync.back.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportTemplateUpdateRequest(
		@NotBlank(message = "템플릿 이름은 필수입니다.")
		@Size(max = 200)
		String name
) {
}
