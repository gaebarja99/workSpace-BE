package com.teamsync.back.report.export.dto;

import jakarta.validation.constraints.Email;
import java.util.List;

/**
 * FR-409 POST /reports/weekly/{reportId}/export/email, POST /reports/team/export/email?weekStart=
 * 요청 바디. recipients가 비어있거나 없으면
 * 서비스 계층에서 기본 수신자(개인 보고서: 작성자 본인 이메일, 팀 보고서: 요청자 본인 이메일)로
 * 대체한다(계약: "비어있으면 작성자 본인 이메일").
 */
public record EmailExportRequest(
		List<@Email(message = "올바른 이메일 형식이 아닙니다.") String> recipients
) {
}
