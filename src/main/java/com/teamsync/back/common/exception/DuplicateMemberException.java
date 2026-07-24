package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/** 구성원 관리(P1): 이미 워크스페이스 구성원인 이메일로 초대를 생성하려는 경우. */
public class DuplicateMemberException extends BusinessException {
	public DuplicateMemberException(String email) {
		super(HttpStatus.CONFLICT, "DUPLICATE_MEMBER", "이미 워크스페이스 구성원인 이메일입니다: " + email);
	}
}
