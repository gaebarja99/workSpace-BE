package com.teamsync.back.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportTemplateSectionUpdateRequest(
		@NotBlank(message = "섹션 제목은 필수입니다.")
		@Size(max = 200)
		String title
) {
}
