package com.teamsync.back.report.entry.dto;

import com.teamsync.back.report.dashboard.dto.MemberSubmissionStatus;

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
