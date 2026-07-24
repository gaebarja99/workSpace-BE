package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-106: @Valid(Bean Validation)로 표현하기 어려운 반복 태스크 템플릿 검증 규칙 위반 시 사용.
 * 예: recurrenceType=WEEKLY인데 dayOfMonth가 함께 오거나 dayOfWeek가 없는 경우, assigneeIds가
 * 워크스페이스에 속하지 않는 경우. InvalidTaskRequestException과 동일하게 "VALIDATION_ERROR" 코드를 사용한다.
 */
public class InvalidRecurringTaskTemplateRequestException extends BusinessException {
	public InvalidRecurringTaskTemplateRequestException(String message) {
		super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
	}
}
