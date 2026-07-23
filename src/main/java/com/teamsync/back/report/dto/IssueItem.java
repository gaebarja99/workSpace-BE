package com.teamsync.back.report.dto;

import java.time.LocalDate;

/**
 * FR-401 "이슈" 섹션 1건. kind=OVERDUE일 때만 daysOverdue가 채워지고(dueDate 대비 지연 일수),
 * kind=STALE일 때만 staleSinceDate가 채워진다(마지막 업데이트 날짜 = 정체가 시작된 날짜의 근사).
 */
public record IssueItem(
		Long taskId,
		String title,
		IssueKind kind,
		LocalDate dueDate,
		Long daysOverdue,
		LocalDate staleSinceDate
) {
}
