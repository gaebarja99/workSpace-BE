package com.teamsync.back.report.export.dto;

import com.teamsync.back.report.dto.WeeklyReportResponse;

/**
 * FR-409 개인 보고서 내보내기(PDF/이메일/xlsx) 전용 조합 뷰. WeeklyReportResponse는 표시용 작성자
 * 이름/이메일을 담지 않아, WeeklyReportService -> ReportExportService/ReportExcelExportService로
 * 전달할 때 함께 담는다(V23: project 종속 제거로 projectName 필드는 더 이상 없음).
 */
public record WeeklyReportExportView(
		WeeklyReportResponse report,
		String authorName,
		String authorEmail
) {
}
