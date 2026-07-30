package com.teamsync.back.report.export;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.report.export.ReportExcelExportService.ReportExcelFile;
import com.teamsync.back.report.export.ReportExportService.ReportPdfFile;
import com.teamsync.back.report.export.dto.EmailExportRequest;
import com.teamsync.back.report.export.dto.EmailExportResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-409(P3, 보고서 내보내기: PDF 다운로드/이메일 발송) API(V23 재설계 이후에도 경로/권한 모델 유지).
 * 개인 보고서는 reportId로 직접 접근("본인 또는 같은 워크스페이스 LEADER/ADMIN"이라는, project 범위로는
 * 표현 못 하는 권한 모델이라 WeeklyReportService.getReportForExport가 서비스 계층에서 검증하고, 그
 * 외에는 403 대신 404로 응답한다). 팀 보고서는 더 이상 발행 레코드가 없으므로 teamReportId 대신
 * weekStart 쿼리 파라미터로 식별한다(ADMIN/LEADER만 허용).
 */
@RestController
@RequestMapping("/api/reports")
public class ReportExportController {

	private static final MediaType XLSX_MEDIA_TYPE =
			MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final ReportExportService reportExportService;
	private final ReportExcelExportService reportExcelExportService;

	public ReportExportController(ReportExportService reportExportService,
			ReportExcelExportService reportExcelExportService) {
		this.reportExportService = reportExportService;
		this.reportExcelExportService = reportExcelExportService;
	}

	@GetMapping("/weekly/{reportId}/export/pdf")
	public ResponseEntity<byte[]> exportWeeklyPdf(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long reportId) {
		return pdfResponse(reportExportService.exportIndividualPdf(principal, reportId));
	}

	@GetMapping("/team/export/pdf")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<byte[]> exportTeamPdf(@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(required = false) LocalDate weekStart) {
		return pdfResponse(reportExportService.exportTeamPdf(principal, weekStart));
	}

	@PostMapping("/weekly/{reportId}/export/email")
	public ResponseEntity<EmailExportResponse> emailWeeklyReport(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long reportId, @Valid @RequestBody(required = false) EmailExportRequest request) {
		return ResponseEntity.ok(reportExportService.emailIndividualReport(principal, reportId, recipientsOf(request)));
	}

	@PostMapping("/team/export/email")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<EmailExportResponse> emailTeamReport(@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(required = false) LocalDate weekStart,
			@Valid @RequestBody(required = false) EmailExportRequest request) {
		return ResponseEntity.ok(reportExportService.emailTeamReport(principal, weekStart, recipientsOf(request)));
	}

	// ----- xlsx 내보내기(Apache POI, 저장 없이 즉석 렌더링) -----

	@GetMapping("/weekly/{reportId}/export/xlsx")
	public ResponseEntity<byte[]> exportWeeklyExcel(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long reportId) {
		return excelResponse(reportExcelExportService.exportIndividualExcel(principal, reportId));
	}

	@GetMapping("/team/export/xlsx")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<byte[]> exportTeamExcel(@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(required = false) LocalDate weekStart) {
		return excelResponse(reportExcelExportService.exportTeamExcel(principal, weekStart));
	}

	@GetMapping("/executive/export/xlsx")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<byte[]> exportExecutiveExcel(@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestParam(required = false) LocalDate weekStart) {
		return excelResponse(reportExcelExportService.exportExecutiveExcel(principal, weekStart));
	}

	private static List<String> recipientsOf(EmailExportRequest request) {
		return request != null ? request.recipients() : null;
	}

	private static ResponseEntity<byte[]> pdfResponse(ReportPdfFile file) {
		ContentDisposition disposition = ContentDisposition.attachment()
				.filename(file.filename(), StandardCharsets.UTF_8)
				.build();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDisposition(disposition);
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(file.bytes());
	}

	private static ResponseEntity<byte[]> excelResponse(ReportExcelFile file) {
		ContentDisposition disposition = ContentDisposition.attachment()
				.filename(file.filename(), StandardCharsets.UTF_8)
				.build();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDisposition(disposition);
		return ResponseEntity.ok().headers(headers).contentType(XLSX_MEDIA_TYPE).body(file.bytes());
	}
}
