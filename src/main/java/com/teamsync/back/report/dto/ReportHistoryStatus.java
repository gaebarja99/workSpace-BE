package com.teamsync.back.report.dto;

/**
 * GET /reports/history 응답의 주차별 상태. TeamWeeklyReport 레코드 존재 여부로만 판정한다.
 */
public enum ReportHistoryStatus {
	AGGREGATING,
	PUBLISHED
}
