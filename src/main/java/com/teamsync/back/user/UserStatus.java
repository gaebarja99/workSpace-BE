package com.teamsync.back.user;

/**
 * 구성원 관리(P1): 워크스페이스 내 계정의 활성 상태.
 * DEACTIVATED 계정은 로그인/토큰 발급이 차단된다(AuthService.login, SsoService.exchange).
 * DB(users.status) CHECK 제약과 값이 정확히 일치해야 한다(V14).
 */
public enum UserStatus {
	ACTIVE,
	DEACTIVATED
}
