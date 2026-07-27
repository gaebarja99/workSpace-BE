package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-409(보고서 내보내기): 요청한 개인 주간 보고서가 존재하지 않거나, 다른 워크스페이스 소속이거나,
 * 요청자가 본인도 아니고 같은 워크스페이스 ADMIN/LEADER도 아닌 경우. 세 경우 모두 403이 아닌 404로
 * 응답해 리소스 존재 여부 자체를 숨긴다(ProjectNotFoundException과 동일 원칙, PRD 5.6 리스크 대응).
 */
public class WeeklyReportNotFoundException extends BusinessException {
	public WeeklyReportNotFoundException() {
		super(HttpStatus.NOT_FOUND, "WEEKLY_REPORT_NOT_FOUND", "주간 보고서를 찾을 수 없습니다.");
	}
}
