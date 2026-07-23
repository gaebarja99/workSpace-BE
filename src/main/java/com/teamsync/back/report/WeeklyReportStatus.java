package com.teamsync.back.report;

/**
 * FR-401/403(개인 주간 보고서): DRAFT(작성 중, 자동저장/수동 초안저장 대상) -> SUBMITTED(팀장에게 제출,
 * 이후 nextWeekPlan 수정 불가) 단방향 전이만 허용한다(계약 문서: "확정 이후 원본 불변" 원칙).
 */
public enum WeeklyReportStatus {
	DRAFT,
	SUBMITTED
}
