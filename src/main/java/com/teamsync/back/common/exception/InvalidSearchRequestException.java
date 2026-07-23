package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-004(통합 검색) 검증 규칙 위반 시 사용. 예: 검색어(q)가 없거나 공백만 있는 경우.
 * GlobalExceptionHandler의 MethodArgumentNotValidException 처리와 동일한 코드("VALIDATION_ERROR")를 사용해
 * 클라이언트가 하나의 에러 코드로 검증 실패를 판별할 수 있도록 한다.
 */
public class InvalidSearchRequestException extends BusinessException {
	public InvalidSearchRequestException(String message) {
		super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
	}
}
