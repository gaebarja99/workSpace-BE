package com.teamsync.back.user;

/**
 * FR-002: 계정의 인증 출처.
 * LOCAL(이메일+비밀번호 가입) 외에 SSO(Google/Microsoft) 및 QA E2E 검증용 MOCK을 구분한다.
 * DB(users.auth_provider) CHECK 제약과 값이 정확히 일치해야 한다(V12).
 */
public enum AuthProvider {
	LOCAL,
	GOOGLE,
	MICROSOFT,
	MOCK
}
