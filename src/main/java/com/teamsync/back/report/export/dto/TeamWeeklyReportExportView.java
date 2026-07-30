package com.teamsync.back.report.export.dto;

import com.teamsync.back.report.dashboard.dto.TeamDashboardResponse;

/**
 * FR-409 팀 보고서 내보내기(PDF/이메일/xlsx) 전용 조합 뷰. V23부터 "발행(TeamWeeklyReport)" 개념이
 * 없으므로 GET /reports/team과 동일한 실시간 집계 결과(TeamDashboardResponse)를 그대로 감싼다.
 */
public record TeamWeeklyReportExportView(
		TeamDashboardResponse report
) {
}
