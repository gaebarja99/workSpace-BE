package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * @Valid(Bean Validation)로 표현하기 어려운 부분 업데이트(PATCH) 검증 규칙 위반 시 사용.
 * 예: PATCH 요청에서 assigneeIds가 명시적으로 빈 리스트로 온 경우, title/content가 공백만 있는 경우.
 * GlobalExceptionHandler의 MethodArgumentNotValidException 처리와 동일한 코드("VALIDATION_ERROR")를 사용해
 * 클라이언트가 하나의 에러 코드로 검증 실패를 판별할 수 있도록 한다.
 */
public class InvalidTaskRequestException extends BusinessException {
	public InvalidTaskRequestException(String message) {
		super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
	}
}
