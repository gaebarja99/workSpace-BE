package com.teamsync.back.report;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.email.EmailSender;
import com.teamsync.back.report.dto.EmailExportResponse;
import com.teamsync.back.report.dto.TeamWeeklyReportExportView;
import com.teamsync.back.report.dto.WeeklyReportExportView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * FR-409(P3, 보고서 내보내기: PDF 다운로드/이메일 발송, V23 재설계 이후에도 유지). 권한/워크스페이스
 * 스코핑 검증과 응답 DTO 계산은 WeeklyReportService(getReportForExport/getTeamReportForExport)에
 * 위임하고, 이 서비스는 "그 결과를 어떻게 내보낼지"(PDF 렌더링, 이메일 발송)만 담당한다. 팀 보고서는
 * 더 이상 발행 레코드(teamReportId)가 없으므로 weekStart로 식별한다.
 */
@Service
public class ReportExportService {

	private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final WeeklyReportService weeklyReportService;
	private final ReportPdfRenderer pdfRenderer;
	private final EmailSender emailSender;

	public ReportExportService(WeeklyReportService weeklyReportService, ReportPdfRenderer pdfRenderer,
			EmailSender emailSender) {
		this.weeklyReportService = weeklyReportService;
		this.pdfRenderer = pdfRenderer;
		this.emailSender = emailSender;
	}

	public ReportPdfFile exportIndividualPdf(AuthenticatedUser principal, Long reportId) {
		WeeklyReportExportView view = weeklyReportService.getReportForExport(principal, reportId);
		byte[] pdf = pdfRenderer.render(ReportExportHtmlBuilder.buildIndividualPdfHtml(view));
		return new ReportPdfFile(fileName(view.authorName(), view.report().weekStart()), pdf);
	}

	public ReportPdfFile exportTeamPdf(AuthenticatedUser principal, LocalDate weekStart) {
		TeamWeeklyReportExportView view = weeklyReportService.getTeamReportForExport(principal, weekStart);
		byte[] pdf = pdfRenderer.render(ReportExportHtmlBuilder.buildTeamPdfHtml(view));
		return new ReportPdfFile(fileName("팀_주간보고", view.report().weekStart()), pdf);
	}

	/**
	 * FR-409: recipients가 비어있으면 "작성자 본인 이메일"로 대체한다(계약). 발송 실패는 조용히 삼키지
	 * 않는다 — API 응답(sentTo)이 실제 발송 대상을 그대로 반영해야 하므로, 실패 시 그대로 500으로
	 * 전파한다.
	 */
	public EmailExportResponse emailIndividualReport(AuthenticatedUser principal, Long reportId,
			List<String> requestedRecipients) {
		WeeklyReportExportView view = weeklyReportService.getReportForExport(principal, reportId);
		List<String> recipients = resolveRecipients(requestedRecipients, view.authorEmail());
		byte[] pdf = pdfRenderer.render(ReportExportHtmlBuilder.buildIndividualPdfHtml(view));
		String html = ReportExportHtmlBuilder.buildIndividualCardEmailHtml(view);
		String subject = subject(view.authorName() + " 주간 보고", view.report().weekStart(), view.report().weekEnd());
		String filename = fileName(view.authorName(), view.report().weekStart());
		sendToAll(recipients, subject, html, filename, pdf);
		return new EmailExportResponse(recipients, emailSender.isMock());
	}

	/** FR-409: 팀 보고서는 "작성자" 개념이 없어(V23도 동일) recipients가 비어있으면 요청자 이메일로 대체한다. */
	public EmailExportResponse emailTeamReport(AuthenticatedUser principal, LocalDate weekStart,
			List<String> requestedRecipients) {
		TeamWeeklyReportExportView view = weeklyReportService.getTeamReportForExport(principal, weekStart);
		List<String> recipients = resolveRecipients(requestedRecipients, principal.email());
		byte[] pdf = pdfRenderer.render(ReportExportHtmlBuilder.buildTeamPdfHtml(view));
		String html = ReportExportHtmlBuilder.buildTeamCardEmailHtml(view);
		String subject = subject("팀 주간 보고", view.report().weekStart(), view.report().weekEnd());
		String filename = fileName("팀_주간보고", view.report().weekStart());
		sendToAll(recipients, subject, html, filename, pdf);
		return new EmailExportResponse(recipients, emailSender.isMock());
	}

	private void sendToAll(List<String> recipients, String subject, String html, String filename, byte[] pdf) {
		for (String recipient : recipients) {
			emailSender.sendHtmlWithAttachment(recipient, subject, html, filename, pdf, "application/pdf");
		}
	}

	private static List<String> resolveRecipients(List<String> requested, String fallbackEmail) {
		if (requested == null || requested.isEmpty()) {
			return List.of(fallbackEmail);
		}
		return requested.stream().filter(StringUtils::hasText).distinct().toList();
	}

	private static String subject(String label, LocalDate weekStart, LocalDate weekEnd) {
		return "[TeamSync] " + label + " (" + weekStart + " ~ " + weekEnd + ")";
	}

	private static String fileName(String label, LocalDate weekStart) {
		String safe = label == null ? "report" : label.replaceAll("[\\\\/:*?\"<>|]", "_");
		return safe + "_" + weekStart.format(FILE_DATE) + ".pdf";
	}

	/** GET .../export/pdf 컨트롤러 응답 조립용(다운로드 파일명 + PDF 바이트). */
	public record ReportPdfFile(String filename, byte[] bytes) {
	}
}
