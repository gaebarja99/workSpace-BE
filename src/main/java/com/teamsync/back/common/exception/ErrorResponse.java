package com.teamsync.back.common.exception;

import java.time.LocalDateTime;

/**
 * 공통 에러 응답 포맷. 모든 4xx/5xx 응답이 동일한 구조를 갖도록 한다.
 */
public record ErrorResponse(
		LocalDateTime timestamp,
		int status,
		String code,
		String message,
		String path
) {
	public static ErrorResponse of(int status, String code, String message, String path) {
		return new ErrorResponse(LocalDateTime.now(), status, code, message, path);
	}
}
