package com.teamsync.back.report.dto;

/**
 * FR-409 개인 보고서 내보내기(PDF/이메일) 전용 조합 뷰. WeeklyReportResponse는 REST 응답 포맷상
 * projectId만 담고 있어 PDF/이메일 렌더링에 필요한 표시용 이름(프로젝트명·작성자명·작성자 이메일)을
 * 함께 담아 WeeklyReportService -> ReportExportService로 전달한다.
 */
public record WeeklyReportExportView(
		WeeklyReportResponse report,
		String projectName,
		String authorName,
		String authorEmail
) {
}
