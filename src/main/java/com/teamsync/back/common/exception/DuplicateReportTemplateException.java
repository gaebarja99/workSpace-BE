package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-405: 이미 해당 범위(워크스페이스 전사 기본 또는 특정 프로젝트 전용)에 템플릿이 존재하는데
 * 다시 생성을 시도한 경우(V20 부분 유니크 인덱스와 동일한 규칙을 애플리케이션 계층에서 선제 검증).
 */
public class DuplicateReportTemplateException extends BusinessException {
	public DuplicateReportTemplateException() {
		super(HttpStatus.CONFLICT, "DUPLICATE_REPORT_TEMPLATE", "이미 해당 범위에 보고서 템플릿이 존재합니다.");
	}
}
