package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/** 구성원 관리(P1): 관리자가 자기 자신의 계정을 비활성화하려는 경우. */
public class SelfDeactivationException extends BusinessException {
	public SelfDeactivationException() {
		super(HttpStatus.BAD_REQUEST, "SELF_DEACTIVATION", "자기 자신의 계정은 비활성화할 수 없습니다.");
	}
}
