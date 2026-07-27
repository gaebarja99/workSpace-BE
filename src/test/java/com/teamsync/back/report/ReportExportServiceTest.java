package com.teamsync.back.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.email.EmailSender;
import com.teamsync.back.report.ReportExportService.ReportPdfFile;
import com.teamsync.back.report.dto.EmailExportResponse;
import com.teamsync.back.report.dto.ReportEntries;
import com.teamsync.back.report.dto.TeamDashboardResponse;
import com.teamsync.back.report.dto.TeamWeeklyReportExportView;
import com.teamsync.back.report.dto.WeeklyReportExportView;
import com.teamsync.back.report.dto.WeeklyReportResponse;
import com.teamsync.back.user.Role;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * FR-409(보고서 내보내기) 서비스 단위 테스트(V23 재설계). 실제 PDF 렌더링/이메일 발송은 mock으로
 * 대체하고, (1) recipients 미지정 시 기본 수신자 대체 규칙과 (2) 이메일 발송기의 mock 상태가 응답에
 * 그대로 반영되는지만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ReportExportServiceTest {

	@Mock
	private WeeklyReportService weeklyReportService;

	@Mock
	private ReportPdfRenderer pdfRenderer;

	@Mock
	private EmailSender emailSender;

	private ReportExportService reportExportService;

	private static final LocalDate WEEK_START = LocalDate.of(2020, 1, 6);
	private static final LocalDate WEEK_END = WEEK_START.plusDays(6);

	@BeforeEach
	void setUp() {
		reportExportService = new ReportExportService(weeklyReportService, pdfRenderer, emailSender);
	}

	@Test
	void 개인_보고서_이메일_발송시_recipients_미지정이면_작성자_본인_이메일로_발송된다() {
		AuthenticatedUser principal = new AuthenticatedUser(1L, 10L, "leader@growtech.io", Role.LEADER);
		WeeklyReportExportView view = new WeeklyReportExportView(individualResponse(), "작성자", "author@growtech.io");
		when(weeklyReportService.getReportForExport(principal, 900L)).thenReturn(view);
		when(pdfRenderer.render(anyString())).thenReturn(new byte[] {1, 2, 3});
		when(emailSender.isMock()).thenReturn(true);

		EmailExportResponse response = reportExportService.emailIndividualReport(principal, 900L, null);

		assertThat(response.sentTo()).containsExactly("author@growtech.io");
		assertThat(response.mocked()).isTrue();
		verify(emailSender).sendHtmlWithAttachment(eq("author@growtech.io"), anyString(), anyString(), anyString(),
				any(byte[].class), eq("application/pdf"));
	}

	@Test
	void 팀_보고서_이메일_발송시_recipients_지정하면_그대로_사용된다() {
		AuthenticatedUser principal = new AuthenticatedUser(1L, 10L, "leader@growtech.io", Role.LEADER);
		TeamWeeklyReportExportView view = new TeamWeeklyReportExportView(teamResponse());
		when(weeklyReportService.getTeamReportForExport(principal, WEEK_START)).thenReturn(view);
		when(pdfRenderer.render(anyString())).thenReturn(new byte[] {1, 2, 3});
		when(emailSender.isMock()).thenReturn(false);

		EmailExportResponse response = reportExportService.emailTeamReport(principal, WEEK_START,
				List.of("a@growtech.io", "b@growtech.io"));

		assertThat(response.sentTo()).containsExactly("a@growtech.io", "b@growtech.io");
		assertThat(response.mocked()).isFalse();
		verify(emailSender).sendHtmlWithAttachment(eq("a@growtech.io"), anyString(), anyString(), anyString(),
				any(byte[].class), eq("application/pdf"));
		verify(emailSender).sendHtmlWithAttachment(eq("b@growtech.io"), anyString(), anyString(), anyString(),
				any(byte[].class), eq("application/pdf"));
	}

	@Test
	void PDF_다운로드는_렌더러가_만든_바이트와_파일명을_그대로_반환한다() {
		AuthenticatedUser principal = new AuthenticatedUser(1L, 10L, "leader@growtech.io", Role.LEADER);
		WeeklyReportExportView view = new WeeklyReportExportView(individualResponse(), "작성자", "author@growtech.io");
		when(weeklyReportService.getReportForExport(principal, 900L)).thenReturn(view);
		byte[] pdfBytes = {9, 9, 9};
		when(pdfRenderer.render(anyString())).thenReturn(pdfBytes);

		ReportPdfFile file = reportExportService.exportIndividualPdf(principal, 900L);

		assertThat(file.bytes()).isEqualTo(pdfBytes);
		assertThat(file.filename()).startsWith("작성자").endsWith(".pdf");
	}

	private static WeeklyReportResponse individualResponse() {
		return new WeeklyReportResponse(900L, WEEK_START, WEEK_END, WeeklyReportStatus.SUBMITTED, null, null,
				new ReportEntries(List.of(), List.of()));
	}

	private static TeamDashboardResponse teamResponse() {
		return new TeamDashboardResponse(WEEK_START, WEEK_END, 0, 0, List.of());
	}
}
