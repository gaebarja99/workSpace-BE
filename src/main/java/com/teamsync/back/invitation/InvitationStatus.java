package com.teamsync.back.invitation;

/**
 * 구성원 관리(P1): 초대의 생명주기.
 * PENDING -> ACCEPTED(가입 완료) / REVOKED(관리자 철회) / EXPIRED(만료, 조회 시점 lazy 처리).
 * DB(invitations.status) CHECK 제약과 값이 정확히 일치해야 한다(V14).
 */
public enum InvitationStatus {
	PENDING,
	ACCEPTED,
	REVOKED,
	EXPIRED
}
