package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 요청한 알림이 존재하지 않거나 요청자(recipient) 소유가 아닌 경우.
 * 후자의 경우도 존재 여부 자체를 숨기기 위해 동일하게 404로 응답한다(PRD 5.6 리스크 대응 원칙과 동일).
 */
public class NotificationNotFoundException extends BusinessException {
	public NotificationNotFoundException() {
		super(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다.");
	}
}
