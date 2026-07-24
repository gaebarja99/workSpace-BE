package com.teamsync.back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * FR-407(조직 롤업) 등 주간 보고 관련 쿼리 파라미터 검증 규칙 위반 시 사용. 예: weekStart가 월요일이
 * 아닌 경우(GET /api/reports/rollup 계약: "월요일이 아니면 400"). GlobalExceptionHandler의
 * MethodArgumentNotValidException 처리와 동일한 코드("VALIDATION_ERROR")를 사용해 클라이언트가
 * 하나의 에러 코드로 검증 실패를 판별할 수 있도록 한다.
 */
public class InvalidReportRequestException extends BusinessException {
	public InvalidReportRequestException(String message) {
		super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
	}
}
