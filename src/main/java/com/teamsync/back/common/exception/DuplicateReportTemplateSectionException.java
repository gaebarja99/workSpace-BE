package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/** FR-405: MANUAL이 아닌 표준 섹션 키가 같은 템플릿 안에 이미 존재하는 경우. */
public class DuplicateReportTemplateSectionException extends BusinessException {
	public DuplicateReportTemplateSectionException() {
		super(HttpStatus.CONFLICT, "DUPLICATE_REPORT_TEMPLATE_SECTION", "이미 동일한 종류의 섹션이 존재합니다.");
	}
}
