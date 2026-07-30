package com.teamsync.back.report.entry.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * PUT /api/reports/me/entries 요청 바디 한 행. 대분류는 실제 프로젝트(Project)의 id를 참조하고,
 * 중분류는 활성 CategoryKeyword의 id를 참조한다. WeeklyReportService가 (1) 프로젝트가 요청자
 * 워크스페이스에 속하는지, (2) 중분류 카테고리가 존재/활성이고 MIDDLE 타입인지를 검증한다.
 */
public record EntryUpsertRequest(
		@NotNull Long projectId,
		@NotNull Long middleCategoryId,
		String minorCategory,
		String detail,
		@Min(0) @Max(100) int ratePercent
) {
}
