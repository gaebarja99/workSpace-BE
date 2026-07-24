package com.teamsync.back.report.dto;

import java.time.LocalDate;

/**
 * GET /api/reports/rollup(FR-407)의 조직 전체 완료율 4주 추이 1건. completionRate는 해당 주
 * 워크스페이스 전체(모든 ACTIVE 프로젝트 합산) 완료 태스크 수 / (완료+진행+이슈) 태스크 수의 0~100
 * 정수 반올림 %다.
 */
public record RollupTrendItem(
		LocalDate weekStart,
		LocalDate weekEnd,
		int completionRate
) {
}
