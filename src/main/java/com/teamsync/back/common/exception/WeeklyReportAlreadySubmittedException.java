package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-403: 이미 SUBMITTED 상태인 개인 주간 보고서를 다시 수정(PATCH)하거나 다시 제출(submit)하려는 경우.
 * "확정 이후 원본 불변" 원칙(계약 문서)에 따라 409로 응답한다.
 */
public class WeeklyReportAlreadySubmittedException extends BusinessException {
	public WeeklyReportAlreadySubmittedException() {
		super(HttpStatus.CONFLICT, "WEEKLY_REPORT_ALREADY_SUBMITTED", "이미 제출된 주간 보고서는 수정할 수 없습니다.");
	}
}
