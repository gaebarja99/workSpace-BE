package com.teamsync.back.report.dto;

import java.time.LocalDate;
import java.util.List;

/** GET /api/reports/executive 응답(ADMIN 전용). 전 인원의 entries를 대분류(majorCategory) 기준으로 그룹핑한다. */
public record ExecutiveDashboardResponse(
		LocalDate weekStart,
		LocalDate weekEnd,
		List<ExecutiveCategoryGroup> categories
) {
}
