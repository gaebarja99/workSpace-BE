package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/** 구성원 관리(P1): 워크스페이스의 유일한 활성(ACTIVE) ADMIN을 비활성화하려는 경우. */
public class LastActiveAdminException extends BusinessException {
	public LastActiveAdminException() {
		super(HttpStatus.BAD_REQUEST, "LAST_ACTIVE_ADMIN", "워크스페이스의 유일한 활성 관리자는 비활성화할 수 없습니다.");
	}
}
