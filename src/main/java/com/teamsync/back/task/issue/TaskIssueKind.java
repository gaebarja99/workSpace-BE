package com.teamsync.back.task.issue;

/**
 * FR-406(이슈/리스크 자동 플래그) 종류. OVERDUE/STALE은 FR-401(WeeklyReportService)의 일회성
 * 스냅샷 판정 공식을 그대로 재사용하고, BLOCKED(선행 태스크 미완료)는 이번에 신설한다.
 */
public enum TaskIssueKind {
	OVERDUE,
	STALE,
	BLOCKED
}
