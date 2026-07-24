package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/** 구성원 관리(P1): 비활성화(DEACTIVATED)된 계정으로 로그인/토큰 발급을 시도하는 경우. */
public class AccountDeactivatedException extends BusinessException {
	public AccountDeactivatedException() {
		super(HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED", "비활성화된 계정입니다. 관리자에게 문의하세요.");
	}
}
