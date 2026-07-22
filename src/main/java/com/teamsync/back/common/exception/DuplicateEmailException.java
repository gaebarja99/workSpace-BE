package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends BusinessException {
	public DuplicateEmailException(String email) {
		super(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다: " + email);
	}
}
