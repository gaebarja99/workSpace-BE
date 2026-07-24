package com.teamsync.back.report.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** sectionIds는 해당 템플릿의 전체 섹션 id를 원하는 순서대로 나열한 목록이어야 한다(누락/추가/타 템플릿 id 혼입 시 400). */
public record ReportTemplateSectionOrderRequest(
		@NotEmpty(message = "sectionIds는 최소 1개 이상이어야 합니다.")
		List<Long> sectionIds
) {
}
