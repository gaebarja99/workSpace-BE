package com.teamsync.back.report.dto;

/**
 * GET /reports/team 응답의 멤버별 제출 상태. WeeklyReportStatus(DRAFT/SUBMITTED)와 이름이 다른 이유는
 * "아직 보고서 레코드 자체가 없는" 멤버도 NOT_SUBMITTED로 표현해야 하기 때문이다(DRAFT와 동일하게 취급).
 */
public enum MemberSubmissionStatus {
	SUBMITTED,
	NOT_SUBMITTED
}
