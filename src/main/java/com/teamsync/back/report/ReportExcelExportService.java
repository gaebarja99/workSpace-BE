package com.teamsync.back.report;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.report.dto.ExecutiveDashboardResponse;
import com.teamsync.back.report.dto.TeamWeeklyReportExportView;
import com.teamsync.back.report.dto.WeeklyReportExportView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/**
 * FR-409(xlsx 내보내기, Apache POI). PDF/이메일과 동일하게 "요청 시 즉석 렌더링, 저장하지 않음" 원칙을
 * 따른다. 권한/워크스페이스 스코핑 검증은 PDF와 동일하게 WeeklyReportService에 위임한다.
 */
@Service
public class ReportExcelExportService {

	private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final WeeklyReportService weeklyReportService;

	public ReportExcelExportService(WeeklyReportService weeklyReportService) {
		this.weeklyReportService = weeklyReportService;
	}

	public ReportExcelFile exportIndividualExcel(AuthenticatedUser principal, Long reportId) {
		WeeklyReportExportView view = weeklyReportService.getReportForExport(principal, reportId);
		byte[] bytes = ReportExcelBuilder.buildIndividualWorkbook(view);
		return new ReportExcelFile(fileName(view.authorName(), view.report().weekStart()), bytes);
	}

	public ReportExcelFile exportTeamExcel(AuthenticatedUser principal, LocalDate weekStart) {
		TeamWeeklyReportExportView view = weeklyReportService.getTeamReportForExport(principal, weekStart);
		byte[] bytes = ReportExcelBuilder.buildTeamWorkbook(view);
		return new ReportExcelFile(fileName("팀_주간보고", view.report().weekStart()), bytes);
	}

	public ReportExcelFile exportExecutiveExcel(AuthenticatedUser principal, LocalDate weekStart) {
		ExecutiveDashboardResponse view = weeklyReportService.getExecutiveDashboard(principal, weekStart);
		byte[] bytes = ReportExcelBuilder.buildExecutiveWorkbook(view);
		return new ReportExcelFile(fileName("대표_뷰", view.weekStart()), bytes);
	}

	private static String fileName(String label, LocalDate weekStart) {
		String safe = label == null ? "report" : label.replaceAll("[\\\\/:*?\"<>|]", "_");
		return safe + "_" + weekStart.format(FILE_DATE) + ".xlsx";
	}

	/** GET .../export/xlsx 컨트롤러 응답 조립용(다운로드 파일명 + xlsx 바이트). */
	public record ReportExcelFile(String filename, byte[] bytes) {
	}
}
