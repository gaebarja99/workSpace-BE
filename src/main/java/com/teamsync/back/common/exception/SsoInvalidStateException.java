package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-002 SSO: state(서명 CSRF 방지 토큰) 검증 실패. 서명 위조, 만료(5분 초과),
 * provider/redirectUri 불일치인 경우.
 */
public class SsoInvalidStateException extends BusinessException {
	public SsoInvalidStateException() {
		super(HttpStatus.BAD_REQUEST, "SSO_INVALID_STATE",
				"SSO 인증 상태(state)가 유효하지 않거나 만료되었습니다. 로그인을 다시 시도하세요.");
	}
}
