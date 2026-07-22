package com.teamsync.back.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamsync.back.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 역할 기반 접근 제어(FR-002)에서 권한이 부족할 때 403 응답을 공통 포맷으로 내려준다. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public RestAccessDeniedHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
			throws IOException {
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		ErrorResponse body = ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "ACCESS_DENIED",
				"권한이 없습니다.", request.getRequestURI());
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
