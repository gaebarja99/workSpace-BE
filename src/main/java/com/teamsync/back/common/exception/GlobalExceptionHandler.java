package com.teamsync.back.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 공통 예외 처리. Spring Security 필터 체인(FilterSecurityInterceptor) 단계에서 발생하는
 * 인증/인가 예외는 이 ControllerAdvice 이전에 처리되어 RestAuthenticationEntryPoint /
 * RestAccessDeniedHandler로 간다. 반면 @PreAuthorize 같은 메서드 보안은 컨트롤러 호출 내부
 * (DispatcherServlet 디스패치 중)에서 AuthorizationDeniedException(AccessDeniedException의
 * 하위 타입)을 던지므로, 여기서 잡지 않으면 catch-all 핸들러가 500으로 처리해버린다.
 * 따라서 이 예외만은 별도로 잡아 "익명 사용자 → 401 UNAUTHENTICATED",
 * "인증된 사용자의 권한 부족 → 403 ACCESS_DENIED"로 필터 단계와 동일하게 구분한다(FR-002).
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
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean anonymous = authentication == null || authentication instanceof AnonymousAuthenticationToken
				|| !authentication.isAuthenticated();
		if (anonymous) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "UNAUTHENTICATED", "인증이 필요합니다.",
							request.getRequestURI()));
		}
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "ACCESS_DENIED", "권한이 없습니다.", request.getRequestURI()));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex,
			HttpServletRequest request) {
		log.warn("MaxUploadSizeExceededException: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "FILE_TOO_LARGE",
						"업로드 파일 크기가 허용된 최대 용량(20MB)을 초과했습니다.", request.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected exception", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR",
						"서버 내부 오류가 발생했습니다.", request.getRequestURI()));
	}
}
