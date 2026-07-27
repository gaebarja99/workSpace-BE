package com.teamsync.back.report.dto;

import java.time.LocalDate;

/**
 * GET /reports/history 응답 1건(V23 재설계: project 비종속, "발행" 개념 제거로 status/issueCount 삭제).
 * completionRate는 0.0~1.0 사이의 비율(submittedCount/totalMemberCount)이며, 퍼센트 표시가 필요하면
 * 프론트에서 *100 처리한다.
 */
public record ReportHistoryItem(
		LocalDate weekStart,
		LocalDate weekEnd,
		int submittedCount,
		int totalMemberCount,
		double completionRate
) {
}
