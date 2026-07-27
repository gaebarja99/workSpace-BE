package com.teamsync.back.report.dto;

import java.util.List;

/** WeeklyReportResponse.entries: 금주 진행사항 / 차주 진행사항 두 표를 그대로 담는다. */
public record ReportEntries(
		List<EntryResponse> thisWeek,
		List<EntryResponse> nextWeek
) {
}
