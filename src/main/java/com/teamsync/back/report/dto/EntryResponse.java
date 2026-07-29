package com.teamsync.back.report.dto;

import com.teamsync.back.report.WeeklyReportEntry;

/**
 * 주간 보고 항목(대/중/소분류 + 상세업무 + 달성율) 공용 응답 DTO. 개인/팀/대표 뷰가 모두 이 DTO를
 * 그대로 재사용한다(WeeklyReportService 참고).
 */
public record EntryResponse(
		Long id,
		Long projectId,
		String projectName,
		Long middleCategoryId,
		String middleCategoryName,
		String minorCategory,
		String detail,
		int ratePercent,
		int orderIndex
) {
	public static EntryResponse from(WeeklyReportEntry entry) {
		return new EntryResponse(
				entry.getId(),
				entry.getProject().getId(), entry.getProject().getName(),
				entry.getMiddleCategory().getId(), entry.getMiddleCategory().getName(),
				entry.getMinorCategory(), entry.getDetail(), entry.getRatePercent(), entry.getOrderIndex());
	}
}
