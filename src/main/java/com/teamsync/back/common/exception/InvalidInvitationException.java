package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/** 구성원 관리(P1): 초대 토큰 기반 가입 시 토큰이 존재하지 않거나 만료/철회/이미사용된 경우. */
public class InvalidInvitationException extends BusinessException {
	public InvalidInvitationException() {
		super(HttpStatus.BAD_REQUEST, "INVALID_INVITATION", "유효하지 않은 초대입니다.");
	}
}
