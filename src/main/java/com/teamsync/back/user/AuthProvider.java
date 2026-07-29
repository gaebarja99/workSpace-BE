package com.teamsync.back.user;

/**
 * FR-002: 계정의 인증 출처.
 * SSO 로그인 기능은 제품 결정으로 제거되어 이메일+비밀번호 가입(LOCAL)만 존재한다.
 * DB(users.auth_provider) CHECK 제약과 값이 정확히 일치해야 한다(V12, V29에서 LOCAL로 축소).
 */
public enum AuthProvider {
	LOCAL
}
