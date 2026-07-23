package com.teamsync.back.auth.sso;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teamsync.back.auth.JwtProperties;
import com.teamsync.back.common.exception.SsoInvalidStateException;
import org.junit.jupiter.api.Test;

/**
 * Spring 컨텍스트 없이 state 서명/검증 로직만 검증하는 단위 테스트(JwtTokenProviderTest 스타일).
 */
class SsoStateServiceTest {

	private final JwtProperties jwtProperties = new JwtProperties(
			"test-only-secret-key-must-be-at-least-32-bytes-long!!", 60_000L);
	private final SsoStateService ssoStateService = new SsoStateService(jwtProperties);

	@Test
	void sign_후_동일_provider_redirectUri면_검증에_성공한다() {
		String state = ssoStateService.sign("google", "http://localhost:3000/api/auth/sso/google/callback");

		assertThatCode(() -> ssoStateService.verify(state, "google",
				"http://localhost:3000/api/auth/sso/google/callback"))
				.doesNotThrowAnyException();
	}

	@Test
	void provider가_다르면_검증에_실패한다() {
		String state = ssoStateService.sign("google", "http://localhost:3000/callback");

		assertThatThrownBy(() -> ssoStateService.verify(state, "microsoft", "http://localhost:3000/callback"))
				.isInstanceOf(SsoInvalidStateException.class);
	}

	@Test
	void redirectUri가_다르면_검증에_실패한다() {
		String state = ssoStateService.sign("google", "http://localhost:3000/callback");

		assertThatThrownBy(() -> ssoStateService.verify(state, "google", "http://evil.example/callback"))
				.isInstanceOf(SsoInvalidStateException.class);
	}

	@Test
	void 위조되거나_빈_state는_검증에_실패한다() {
		assertThatThrownBy(() -> ssoStateService.verify("not-a-valid-jwt", "google", "http://localhost:3000/callback"))
				.isInstanceOf(SsoInvalidStateException.class);
		assertThatThrownBy(() -> ssoStateService.verify("", "google", "http://localhost:3000/callback"))
				.isInstanceOf(SsoInvalidStateException.class);
	}

	@Test
	void 다른_키로_서명된_state는_검증에_실패한다() {
		SsoStateService other = new SsoStateService(
				new JwtProperties("another-different-secret-key-at-least-32bytes-long!!", 60_000L));
		String forged = other.sign("google", "http://localhost:3000/callback");

		assertThatThrownBy(() -> ssoStateService.verify(forged, "google", "http://localhost:3000/callback"))
				.isInstanceOf(SsoInvalidStateException.class);
	}
}
