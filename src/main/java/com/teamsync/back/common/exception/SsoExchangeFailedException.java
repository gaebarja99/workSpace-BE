package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-002 SSO: provider의 code->token 교환 또는 userinfo 조회가 실패한 경우
 * (잘못된 code, provider 오류/응답 파싱 실패 등).
 */
public class SsoExchangeFailedException extends BusinessException {
	public SsoExchangeFailedException(String message) {
		super(HttpStatus.BAD_REQUEST, "SSO_EXCHANGE_FAILED",
				message == null || message.isBlank() ? "SSO 인증 코드 교환에 실패했습니다." : message);
	}
}
