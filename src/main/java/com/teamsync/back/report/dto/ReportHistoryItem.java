package com.teamsync.back.report.dto;

import java.time.LocalDate;

/**
 * GET /reports/history 응답 1건. completionRate는 0.0~1.0 사이의 비율(submittedCount/totalMemberCount)이며,
 * 퍼센트 표시가 필요하면 프론트에서 *100 처리한다(별도 백분율 필드를 추가하지 않음, FR-208 선례와 동일하게
 * 클라이언트 계산으로 충분한 범위는 새 필드를 늘리지 않는다는 원칙).
 */
public record ReportHistoryItem(
		LocalDate weekStart,
		LocalDate weekEnd,
		ReportHistoryStatus status,
		int submittedCount,
		int totalMemberCount,
		double completionRate,
		int issueCount
) {
}
