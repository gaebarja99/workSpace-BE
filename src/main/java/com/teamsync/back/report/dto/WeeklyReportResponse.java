package com.teamsync.back.report.dto;

import com.teamsync.back.report.WeeklyReportStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GET/PATCH /reports/me, POST /reports/me/submit 공통 응답. lastSavedAt은 WeeklyReport.updatedAt
 * 그대로다(자동저장/수동 초안저장 모두 이 컬럼을 갱신하므로 "마지막 자동 저장" 시각으로 그대로 노출 가능).
 * completedTasks/inProgressTasks/highlights/issues는 저장된 값이 아니라 매 요청마다 실시간 계산된다.
 */
public record WeeklyReportResponse(
		Long id,
		Long projectId,
		LocalDate weekStart,
		LocalDate weekEnd,
		WeeklyReportStatus status,
		String nextWeekPlan,
		LocalDateTime submittedAt,
		LocalDateTime lastSavedAt,
		List<CompletedTaskItem> completedTasks,
		List<InProgressTaskItem> inProgressTasks,
		List<HighlightItem> highlights,
		List<IssueItem> issues
) {
}
