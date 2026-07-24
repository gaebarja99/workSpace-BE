package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/** 구성원 관리(P1): 워크스페이스의 유일한 ADMIN을 다른 역할로 강등시키려는 경우. */
public class LastAdminDemotionException extends BusinessException {
	public LastAdminDemotionException() {
		super(HttpStatus.BAD_REQUEST, "LAST_ADMIN_DEMOTION", "워크스페이스의 유일한 관리자는 강등할 수 없습니다.");
	}
}
