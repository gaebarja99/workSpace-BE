package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/** 구성원 관리(P1): 초대 대상 역할로 ADMIN을 지정하려는 경우(LEADER/MEMBER/GUEST만 허용). */
public class InvalidInvitationRoleException extends BusinessException {
	public InvalidInvitationRoleException() {
		super(HttpStatus.BAD_REQUEST, "INVALID_INVITATION_ROLE", "ADMIN 역할로는 초대할 수 없습니다.");
	}
}
