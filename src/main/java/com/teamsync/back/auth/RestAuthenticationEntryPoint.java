package com.teamsync.back.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamsync.back.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증되지 않은 요청(토큰 없음/만료/위조)에 대해 GlobalExceptionHandler와 동일한
 * ErrorResponse 포맷의 401 응답을 내려준다. 필터 단계 예외는 @RestControllerAdvice가
 * 잡지 못하므로 별도 처리한다.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		ErrorResponse body = ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "UNAUTHENTICATED",
				"인증이 필요합니다.", request.getRequestURI());
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
