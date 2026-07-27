package com.teamsync.back.report.dto;

import java.time.LocalDateTime;
import java.util.List;

/** GET /reports/team 응답의 멤버 1명 항목. */
public record TeamMemberReportEntries(
		Long userId,
		String name,
		MemberSubmissionStatus status,
		LocalDateTime submittedAt,
		List<EntryResponse> thisWeekEntries,
		List<EntryResponse> nextWeekEntries
) {
}
