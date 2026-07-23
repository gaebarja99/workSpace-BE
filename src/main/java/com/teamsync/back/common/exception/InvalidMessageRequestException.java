package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * @Valid(Bean Validation)로 표현하기 어려운 메시지 요청 검증 규칙 위반 시 사용.
 * 예: parentMessageId가 존재하지 않거나 다른 채널에 속한 메시지를 가리키는 경우.
 * GlobalExceptionHandler의 MethodArgumentNotValidException 처리와 동일한 코드("VALIDATION_ERROR")를 사용해
 * 클라이언트가 하나의 에러 코드로 검증 실패를 판별할 수 있도록 한다.
 */
public class InvalidMessageRequestException extends BusinessException {
	public InvalidMessageRequestException(String message) {
		super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
	}
}
