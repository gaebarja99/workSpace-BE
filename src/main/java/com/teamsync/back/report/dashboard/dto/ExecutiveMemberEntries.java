package com.teamsync.back.report.dashboard.dto;

import com.teamsync.back.report.entry.dto.EntryResponse;

import java.util.List;

/** GET /api/reports/executive 응답의 대분류 그룹 안 멤버 1명 항목. */
public record ExecutiveMemberEntries(
		Long userId,
		String name,
		List<EntryResponse> thisWeekEntries,
		List<EntryResponse> nextWeekEntries
) {
}
