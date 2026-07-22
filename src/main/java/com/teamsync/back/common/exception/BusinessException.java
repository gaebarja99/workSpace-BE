package com.teamsync.back.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 도메인 규칙 위반 시 공통으로 던지는 예외의 베이스 타입.
 * GlobalExceptionHandler가 이 타입을 잡아 일관된 에러 응답 포맷으로 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public BusinessException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}
}
