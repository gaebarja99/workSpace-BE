package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-405: 요청한 보고서 템플릿이 존재하지 않거나 다른 워크스페이스 소속이라 요청자에게 보이지
 * 않아야 하는 경우.
 */
public class ReportTemplateNotFoundException extends BusinessException {
	public ReportTemplateNotFoundException() {
		super(HttpStatus.NOT_FOUND, "REPORT_TEMPLATE_NOT_FOUND", "보고서 템플릿을 찾을 수 없습니다.");
	}
}
