package com.teamsync.back.auth.sso;

/**
 * FR-002 SSO: Google/Microsoft/Mock을 다형적으로 처리하기 위한 공급자 추상화.
 * 각 구현체가 authorization URL 빌드, code->token 교환, userinfo 조회를 담당한다.
 */
public interface SsoProvider {

	/** 경로/설정에서 사용하는 공급자 키(google | microsoft | mock). */
	String name();

	/** client-id/secret(또는 mock enabled) 설정 여부. false면 사용 불가. */
	boolean isEnabled();

	/** OAuth2 Authorization Code 요청 URL. state는 서명된 CSRF 방지 토큰. */
	String buildAuthorizationUrl(String redirectUri, String signedState);

	/** code를 access token으로 교환하고 userinfo(email, name)를 조회한다. */
	SsoUserInfo exchange(String code, String redirectUri);
}
