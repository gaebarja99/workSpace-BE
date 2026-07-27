package com.teamsync.back.report.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * PUT /api/reports/me/entries 요청 바디 한 행. 대분류/중분류는 활성 CategoryKeyword의 id를 참조하며,
 * WeeklyReportService가 (1) 존재/활성 여부, (2) major/middle 필드에 타입이 맞는 카테고리가 들어왔는지
 * (예: 대분류 필드에 MIDDLE id 입력)를 검증한다.
 */
public record EntryUpsertRequest(
		@NotNull Long majorCategoryId,
		@NotNull Long middleCategoryId,
		String minorCategory,
		String detail,
		@Min(0) @Max(100) int ratePercent
) {
}
