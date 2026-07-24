package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/** 구성원 관리(P1): 초대가 존재하지 않거나(id) 다른 워크스페이스 소속인 경우. */
public class InvitationNotFoundException extends BusinessException {
	public InvitationNotFoundException() {
		super(HttpStatus.NOT_FOUND, "INVITATION_NOT_FOUND", "초대를 찾을 수 없습니다.");
	}
}
