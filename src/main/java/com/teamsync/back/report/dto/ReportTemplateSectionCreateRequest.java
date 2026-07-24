package com.teamsync.back.report.dto;

import com.teamsync.back.report.ReportSectionKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportTemplateSectionCreateRequest(
		@NotNull(message = "섹션 종류(sectionKey)는 필수입니다.")
		ReportSectionKey sectionKey,

		@NotBlank(message = "섹션 제목은 필수입니다.")
		@Size(max = 200)
		String title
) {
}
