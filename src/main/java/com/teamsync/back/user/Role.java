package com.teamsync.back.user;

/**
 * FR-002: 역할 기반 권한. 워크스페이스 내에서 사용자에게 부여되는 역할.
 * PRD v2.0 기준 5단계: ADMIN(관리자) > LEADER(팀장) > MANAGER(과장) / ASSISTANT_MANAGER(대리) /
 * STAFF(사원) > GUEST(게스트).
 * MANAGER/ASSISTANT_MANAGER/STAFF는 PRD 4장 권한 매트릭스상 완전히 동일한 권한(모두 '본인 것만'
 * 접근)을 가지는 하위 직급 세분화이며, 권한 로직에서는 항상 동일하게 취급되어야 한다(과거
 * 단일 MEMBER 역할을 세 값으로 분리한 것). GUEST는 PRD 5단계 목록에 없는 별도 시스템 역할로,
 * 여러 기능(예: 주간 보고 대상 산정)에서 "제외" 대상으로만 취급된다.
 * 이번 단계에서는 Spring Security의 ROLE_* 권한 문자열로 매핑해 사용한다.
 */
public enum Role {
	ADMIN,
	LEADER,
	MANAGER,
	ASSISTANT_MANAGER,
	STAFF,
	GUEST
}
