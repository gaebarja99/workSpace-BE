package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-003 PUT /api/notifications/preferences 요청 검증 실패 시 사용.
 * 예: categories 누락, 알 수 없는 category 문자열. 다른 검증 실패와 동일하게 "VALIDATION_ERROR" 코드로 400을 낸다.
 */
public class InvalidNotificationPreferenceException extends BusinessException {
	public InvalidNotificationPreferenceException(String message) {
		super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
	}
}
