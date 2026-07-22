package com.teamsync.back.user;

/**
 * FR-002: 역할 기반 권한. 워크스페이스 내에서 사용자에게 부여되는 역할.
 * ADMIN(관리자) > LEADER(팀장) > MEMBER(멤버) > GUEST(게스트) 순으로 넓은 권한을 가진다는
 * 전제 하에, 이번 단계에서는 Spring Security의 ROLE_* 권한 문자열로 매핑해 사용한다.
 */
public enum Role {
	ADMIN,
	LEADER,
	MEMBER,
	GUEST
}
