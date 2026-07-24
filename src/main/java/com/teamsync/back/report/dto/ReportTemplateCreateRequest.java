package com.teamsync.back.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** projectId=null이면 워크스페이스 전사 기본 템플릿, 값이 있으면 해당 프로젝트 전용 템플릿을 생성한다. */
public record ReportTemplateCreateRequest(
		Long projectId,

		@NotBlank(message = "템플릿 이름은 필수입니다.")
		@Size(max = 200)
		String name
) {
}
