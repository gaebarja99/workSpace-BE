package com.teamsync.back.report.dashboard.dto;

import com.teamsync.back.report.entry.dto.TeamMemberReportEntries;

import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/reports/team 응답(LEADER/ADMIN). 발행(TeamWeeklyReport) 개념 없이 항상 실시간 집계한다.
 */
public record TeamDashboardResponse(
		LocalDate weekStart,
		LocalDate weekEnd,
		int submittedCount,
		int totalMemberCount,
		List<TeamMemberReportEntries> members
) {
}
