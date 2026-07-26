package com.teamsync.back.report.dto;

/**
 * FR-409 팀 보고서 내보내기(PDF/이메일) 전용 조합 뷰. TeamWeeklyReportResponse는 projectId만 담고
 * 있어 표시용 프로젝트명을 함께 담아 WeeklyReportService -> ReportExportService로 전달한다.
 */
public record TeamWeeklyReportExportView(
		TeamWeeklyReportResponse report,
		String projectName
) {
}
