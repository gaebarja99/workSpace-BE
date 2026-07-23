package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * @Valid(Bean Validation)로 표현하기 어려운 DM 요청 검증 규칙 위반 시 사용.
 * 예: participantIds가 비어 있거나(본인 제외 후 0명), 다른 워크스페이스 사용자가 섞여 들어온 경우.
 * GlobalExceptionHandler의 MethodArgumentNotValidException 처리와 동일한 코드("VALIDATION_ERROR")를 사용해
 * 클라이언트가 하나의 에러 코드로 검증 실패를 판별할 수 있도록 한다.
 */
public class InvalidDmRequestException extends BusinessException {
	public InvalidDmRequestException(String message) {
		super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
	}
}
