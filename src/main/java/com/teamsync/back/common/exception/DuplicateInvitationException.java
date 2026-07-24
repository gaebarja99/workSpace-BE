package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/** 구성원 관리(P1): 동일 (workspace, email)에 이미 PENDING(미만료) 초대가 존재하는 경우. */
public class DuplicateInvitationException extends BusinessException {
	public DuplicateInvitationException(String email) {
		super(HttpStatus.CONFLICT, "DUPLICATE_INVITATION", "이미 초대가 진행 중인 이메일입니다: " + email);
	}
}
