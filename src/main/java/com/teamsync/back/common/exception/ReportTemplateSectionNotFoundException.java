package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/** FR-405: 요청한 섹션이 존재하지 않거나 다른 템플릿 소속인 경우. */
public class ReportTemplateSectionNotFoundException extends BusinessException {
	public ReportTemplateSectionNotFoundException() {
		super(HttpStatus.NOT_FOUND, "REPORT_TEMPLATE_SECTION_NOT_FOUND", "템플릿 섹션을 찾을 수 없습니다.");
	}
}
