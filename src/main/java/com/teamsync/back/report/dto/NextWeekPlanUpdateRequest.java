package com.teamsync.back.report.dto;

/**
 * PATCH /reports/me 요청 바디. nextWeekPlan은 null이 와도 빈 문자열로 취급한다(WeeklyReport.changeNextWeekPlan).
 */
public record NextWeekPlanUpdateRequest(
		String nextWeekPlan
) {
}
