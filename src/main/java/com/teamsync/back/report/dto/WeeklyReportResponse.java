package com.teamsync.back.report.dto;

import com.teamsync.back.report.WeeklyReportStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * GET/PUT /reports/me*, POST /reports/me/submit 공통 응답(V23 재설계: project 비종속, user_id +
 * week_start 단위). entries는 저장된 WeeklyReportEntry 행을 section별로 그대로 담는다(더 이상 Task
 * 기반 자동 계산 없음). lastSavedAt은 WeeklyReport.updatedAt 그대로다.
 */
public record WeeklyReportResponse(
		Long id,
		LocalDate weekStart,
		LocalDate weekEnd,
		WeeklyReportStatus status,
		LocalDateTime submittedAt,
		LocalDateTime lastSavedAt,
		ReportEntries entries
) {
}
