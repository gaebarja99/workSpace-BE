package com.teamsync.back.report.dto;

/**
 * POST /reports/team/remind 응답. 실제로 알림이 발송된 인원 수만 반환한다.
 */
public record RemindResponse(
		int notifiedCount
) {
}
