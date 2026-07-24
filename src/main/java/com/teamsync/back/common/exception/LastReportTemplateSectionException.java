package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/** FR-405: 템플릿에 남은 마지막 섹션은 삭제할 수 없다(최소 1개 유지). */
public class LastReportTemplateSectionException extends BusinessException {
	public LastReportTemplateSectionException() {
		super(HttpStatus.CONFLICT, "LAST_REPORT_TEMPLATE_SECTION", "템플릿에는 최소 1개의 섹션이 있어야 합니다.");
	}
}
