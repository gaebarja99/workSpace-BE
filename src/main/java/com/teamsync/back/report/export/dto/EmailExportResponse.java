package com.teamsync.back.report.export.dto;

import java.util.List;

/**
 * FR-409 보고서 이메일 발송 응답. mocked=true는 이메일 발송기가 로그 Mock
 * (LoggingEmailSender)으로 동작 중이라 실제 메일함에는 도달하지 않았음을 뜻한다
 * (email.enabled=false 이거나 SMTP 설정 부재).
 */
public record EmailExportResponse(
		List<String> sentTo,
		boolean mocked
) {
}
