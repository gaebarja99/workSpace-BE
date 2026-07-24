package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/** 구성원 관리(P1): 대상 사용자가 존재하지 않거나 다른 워크스페이스 소속인 경우. */
public class MemberNotFoundException extends BusinessException {
	public MemberNotFoundException() {
		super(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "구성원을 찾을 수 없습니다.");
	}
}
