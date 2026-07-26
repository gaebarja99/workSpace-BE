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
 * FR-409(P3, 보고서 내보내기: PDF 다운로드/이메일 발송). 권한/워크스페이스 스코핑 검증과 응답 DTO
 * 계산은 WeeklyReportService(getReportForExport/getTeamReportForExport)에 위임하고, 이 서비스는
 * "그 결과를 어떻게 내보낼지"(PDF 렌더링, 이메일 발송)만 담당한다. PDF는 요청마다 즉석 렌더링하고
 * 저장하지 않는다(별도 저장소 도입은 이번 스코프에서 과도한 엔지니어링 — 다운로드/발송 시점 스냅샷이면
 * 충분). Notion/Google Docs 내보내기는 배포 환경에 OAuth 자격증명이 없어 이번 세션 스코프에서 제외한다
 * (HANDOFF 문서화, PDF/이메일만 구현).
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
		return new ReportPdfFile(fileName(view.projectName(), view.report().weekStart()), pdf);
	}

	public ReportPdfFile exportTeamPdf(AuthenticatedUser principal, Long teamReportId) {
		TeamWeeklyReportExportView view = weeklyReportService.getTeamReportForExport(principal, teamReportId);
		byte[] pdf = pdfRenderer.render(ReportExportHtmlBuilder.buildTeamPdfHtml(view));
		return new ReportPdfFile(fileName(view.projectName() + "_team", view.report().weekStart()), pdf);
	}

	/**
	 * FR-409: recipients가 비어있으면 "작성자 본인 이메일"로 대체한다(계약). PDF를 첨부하고, 본문은
	 * Slack-style 카드 요약(HTML)으로 구성한다. 이메일 발송 자체는 best-effort로 삼키지 않는다 —
	 * API 응답(sentTo)이 실제 발송 대상을 그대로 반영해야 하므로, 발송이 실패하면 예외가 그대로
	 * 전파되어 500으로 응답한다(발송 결과가 곧 응답 데이터라 조용히 삼킬 수 없다).
	 */
	public EmailExportResponse emailIndividualReport(AuthenticatedUser principal, Long reportId,
			List<String> requestedRecipients) {
		WeeklyReportExportView view = weeklyReportService.getReportForExport(principal, reportId);
		List<String> recipients = resolveRecipients(requestedRecipients, view.authorEmail());
		byte[] pdf = pdfRenderer.render(ReportExportHtmlBuilder.buildIndividualPdfHtml(view));
		String html = ReportExportHtmlBuilder.buildIndividualCardEmailHtml(view);
		String subject = subject(view.projectName(), "주간 보고", view.report().weekStart(), view.report().weekEnd());
		String filename = fileName(view.projectName(), view.report().weekStart());
		sendToAll(recipients, subject, html, filename, pdf);
		return new EmailExportResponse(recipients, emailSender.isMock());
	}

	/**
	 * FR-409: 팀 보고서는 "작성자" 개념이 없어(계약 문서 명시 없음), recipients가 비어있으면 요청자
	 * (LEADER/ADMIN 본인) 이메일로 대체한다.
	 */
	public EmailExportResponse emailTeamReport(AuthenticatedUser principal, Long teamReportId,
			List<String> requestedRecipients) {
		TeamWeeklyReportExportView view = weeklyReportService.getTeamReportForExport(principal, teamReportId);
		List<String> recipients = resolveRecipients(requestedRecipients, principal.email());
		byte[] pdf = pdfRenderer.render(ReportExportHtmlBuilder.buildTeamPdfHtml(view));
		String html = ReportExportHtmlBuilder.buildTeamCardEmailHtml(view);
		String subject = subject(view.projectName(), "팀 주간 보고", view.report().weekStart(), view.report().weekEnd());
		String filename = fileName(view.projectName() + "_team", view.report().weekStart());
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

	private static String subject(String projectName, String label, LocalDate weekStart, LocalDate weekEnd) {
		return "[TeamSync] " + projectName + " " + label + " (" + weekStart + " ~ " + weekEnd + ")";
	}

	private static String fileName(String projectName, LocalDate weekStart) {
		String safe = projectName == null ? "report" : projectName.replaceAll("[\\\\/:*?\"<>|]", "_");
		return safe + "_" + weekStart.format(FILE_DATE) + ".pdf";
	}

	/** GET .../export/pdf 컨트롤러 응답 조립용(다운로드 파일명 + PDF 바이트). */
	public record ReportPdfFile(String filename, byte[] bytes) {
	}
}
