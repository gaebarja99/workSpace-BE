package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-002 SSO: 지원하지 않거나(경로값 오류) client-id/secret 미설정으로 비활성화된 provider로
 * 인증을 시도한 경우.
 */
public class SsoProviderDisabledException extends BusinessException {
	public SsoProviderDisabledException(String provider) {
		super(HttpStatus.BAD_REQUEST, "SSO_PROVIDER_DISABLED",
				"'" + provider + "' SSO 공급자를 사용할 수 없습니다. 관리자에게 설정을 문의하세요.");
	}
}
