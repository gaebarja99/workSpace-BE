package com.teamsync.back.report.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GET/POST /reports/team, /reports/team/publish 공통 응답. publishedAt이 null이면 "집계 중"
 * (아직 TeamWeeklyReport 레코드가 없음), 값이 있으면 "발행 완료"다.
 */
public record TeamWeeklyReportResponse(
		Long projectId,
		LocalDate weekStart,
		LocalDate weekEnd,
		LocalDateTime publishedAt,
		String publishedByName,
		int submittedCount,
		int totalMemberCount,
		int teamCompletedCount,
		int teamIssueCount,
		List<TeamMemberReportSummary> members
) {
}
