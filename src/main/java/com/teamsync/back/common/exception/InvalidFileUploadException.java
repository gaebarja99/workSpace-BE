package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-204: 파일 업로드 요청이 비어있거나(빈 파일) 원본 파일명이 없는 등, multipart 요청이라
 * @Valid(Bean Validation)로 표현하기 어려운 검증 규칙 위반 시 사용한다.
 * GlobalExceptionHandler의 MethodArgumentNotValidException 처리와 동일한 코드("VALIDATION_ERROR")를
 * 사용해 클라이언트가 하나의 에러 코드로 검증 실패를 판별할 수 있도록 한다.
 */
public class InvalidFileUploadException extends BusinessException {
	public InvalidFileUploadException(String message) {
		super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
	}
}
