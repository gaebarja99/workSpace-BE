package com.teamsync.back.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 공통 예외 처리. Spring Security 필터 체인에서 발생하는 인증/인가 예외는 필터 이전 단계라
 * 이 핸들러를 타지 않으므로 RestAuthenticationEntryPoint / RestAccessDeniedHandler에서
 * 동일한 ErrorResponse 포맷으로 별도 처리한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
		log.warn("BusinessException: {} - {}", ex.getCode(), ex.getMessage());
		return ResponseEntity.status(ex.getStatus())
				.body(ErrorResponse.of(ex.getStatus().value(), ex.getCode(), ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", message, request.getRequestURI()));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "ACCESS_DENIED", "권한이 없습니다.", request.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected exception", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR",
						"서버 내부 오류가 발생했습니다.", request.getRequestURI()));
	}
}
