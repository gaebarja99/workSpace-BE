package com.teamsync.back.auth.sso;

import com.teamsync.back.common.exception.SsoExchangeFailedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * FR-002 SSO: 외부 provider 없이 전체 플로우를 검증하기 위한 QA E2E 전용 mock 공급자.
 * SSO_MOCK_ENABLED=true 일 때만 활성화된다.
 *
 * - authorize: 실제 provider로 redirect하지 않고 FE 콜백으로 즉시 되돌리는 URL을 반환한다
 *   ({redirectUri}?code=mock-code&state={state}).
 * - exchange: code가 "mock-code"면 고정 userinfo를 반환한다. email은 SSO_MOCK_EMAIL로 지정한다
 *   (기존 워크스페이스 도메인 이메일이어야 JIT 프로비저닝이 성공한다).
 */
@Component
public class MockSsoProvider implements SsoProvider {

	static final String MOCK_CODE = "mock-code";

	private final SsoProperties.Mock config;

	public MockSsoProvider(SsoProperties properties) {
		this.config = properties.mock();
	}

	@Override
	public String name() {
		return "mock";
	}

	@Override
	public boolean isEnabled() {
		return config != null && config.enabled();
	}

	@Override
	public String buildAuthorizationUrl(String redirectUri, String signedState) {
		return UriComponentsBuilder.fromUriString(redirectUri)
				.queryParam("code", MOCK_CODE)
				.queryParam("state", signedState)
				.build()
				.encode()
				.toUriString();
	}

	@Override
	public SsoUserInfo exchange(String code, String redirectUri) {
		if (!MOCK_CODE.equals(code)) {
			throw new SsoExchangeFailedException("mock SSO는 code='" + MOCK_CODE + "'만 허용합니다.");
		}
		String email = config == null ? null : config.email();
		if (!StringUtils.hasText(email)) {
			throw new SsoExchangeFailedException(
					"mock SSO 이메일이 설정되지 않았습니다. SSO_MOCK_EMAIL을 기존 워크스페이스 도메인 이메일로 지정하세요.");
		}
		String name = email.substring(0, email.indexOf('@'));
		return new SsoUserInfo(email, name);
	}
}
