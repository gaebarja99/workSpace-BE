package com.teamsync.back.report;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.report.ReportExportService.ReportPdfFile;
import com.teamsync.back.report.dto.EmailExportRequest;
import com.teamsync.back.report.dto.EmailExportResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-409(P3, 보고서 내보내기: PDF 다운로드/이메일 발송) API. ReportController(/api/projects/{projectId}
 * /reports)와 달리 이 컨트롤러는 project+weekStart가 아니라 reportId/teamReportId로 직접 접근한다 —
 * 개인 보고서는 "본인 또는 같은 워크스페이스 LEADER/ADMIN"이라는, project 범위만으로는 표현할 수 없는
 * (소유자 여부에 따라 갈리는) 권한 모델이라 project 하위 경로에 자연스럽게 넣기 어렵기 때문이다.
 * RollupReportController와 마찬가지로 /api/reports 최상위에 둔다.
 *
 * 개인 보고서 export 엔드포인트에는 @PreAuthorize를 걸지 않는다(소유자 자기 자신은 역할과 무관하게
 * 항상 허용되어야 하므로 역할 화이트리스트로 표현 불가) — 대신 WeeklyReportService.getReportForExport가
 * "본인 또는 ADMIN/LEADER"를 서비스 계층에서 검증하고, 그 외에는 404(WeeklyReportNotFoundException)로
 * 응답한다(FR-405 선례와 동일하게 403 대신 404). 팀 보고서 export는 GET/POST /reports/team과 동일하게
 * ADMIN/LEADER만 허용한다.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportExportController {

	private final ReportExportService reportExportService;

	public ReportExportController(ReportExportService reportExportService) {
		this.reportExportService = reportExportService;
	}

	@GetMapping("/weekly/{reportId}/export/pdf")
	public ResponseEntity<byte[]> exportWeeklyPdf(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long reportId) {
		return pdfResponse(reportExportService.exportIndividualPdf(principal, reportId));
	}

	@GetMapping("/team/{teamReportId}/export/pdf")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<byte[]> exportTeamPdf(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long teamReportId) {
		return pdfResponse(reportExportService.exportTeamPdf(principal, teamReportId));
	}

	@PostMapping("/weekly/{reportId}/export/email")
	public ResponseEntity<EmailExportResponse> emailWeeklyReport(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long reportId, @Valid @RequestBody(required = false) EmailExportRequest request) {
		return ResponseEntity.ok(reportExportService.emailIndividualReport(principal, reportId, recipientsOf(request)));
	}

	@PostMapping("/team/{teamReportId}/export/email")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
	public ResponseEntity<EmailExportResponse> emailTeamReport(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long teamReportId, @Valid @RequestBody(required = false) EmailExportRequest request) {
		return ResponseEntity.ok(reportExportService.emailTeamReport(principal, teamReportId, recipientsOf(request)));
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
}
