package com.teamsync.back.report;

import com.teamsync.back.report.dto.EntryResponse;
import com.teamsync.back.report.dto.MemberSubmissionStatus;
import com.teamsync.back.report.dto.TeamDashboardResponse;
import com.teamsync.back.report.dto.TeamMemberReportEntries;
import com.teamsync.back.report.dto.TeamWeeklyReportExportView;
import com.teamsync.back.report.dto.WeeklyReportExportView;
import com.teamsync.back.report.dto.WeeklyReportResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * FR-409(보고서 내보내기, V23 재설계): PDF/이메일 본문용 XHTML 문자열을 만드는 순수 빌더(상태 없음,
 * 정적 메서드만). openhtmltopdf는 XML 파서로 문서를 읽으므로 모든 태그가 XHTML로 well-formed해야
 * 한다 — 사용자 입력(상세업무/소분류 등)은 반드시 {@link #escape}를 거친다. 대/중/소분류 + 상세업무 +
 * 달성율 표 형식을 그대로 재현한다(Task 자동 취합 방식 완전 폐기).
 */
final class ReportExportHtmlBuilder {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final int CARD_ITEM_LIMIT = 5;

	private ReportExportHtmlBuilder() {
	}

	// ----- PDF -----

	static String buildIndividualPdfHtml(WeeklyReportExportView view) {
		WeeklyReportResponse r = view.report();
		StringBuilder body = new StringBuilder();
		body.append(pageHeader(escape(view.authorName()) + " 주간 보고", r.weekStart(), r.weekEnd()));
		body.append("<p>작성자: ").append(escape(view.authorName())).append(" (")
				.append(escape(view.authorEmail())).append(")</p>");
		body.append("<p>상태: ").append(r.status() == WeeklyReportStatus.SUBMITTED ? "제출 완료" : "작성 중")
				.append(" · 제출일시: ").append(formatDateTime(r.submittedAt()))
				.append(" · 마지막 저장: ").append(formatDateTime(r.lastSavedAt())).append("</p>");

		body.append(sectionTitle("금주 진행사항 (" + r.entries().thisWeek().size() + "건)"));
		body.append(entryTable(r.entries().thisWeek()));

		body.append(sectionTitle("차주 진행사항 (" + r.entries().nextWeek().size() + "건)"));
		body.append(entryTable(r.entries().nextWeek()));

		return document(body.toString());
	}

	static String buildTeamPdfHtml(TeamWeeklyReportExportView view) {
		TeamDashboardResponse r = view.report();
		StringBuilder body = new StringBuilder();
		body.append(pageHeader("팀 주간 보고", r.weekStart(), r.weekEnd()));
		body.append("<p>제출 현황: ").append(r.submittedCount()).append(" / ").append(r.totalMemberCount())
				.append("명</p>");

		for (TeamMemberReportEntries member : r.members()) {
			body.append(sectionTitle(escape(member.name()) + " ("
					+ (member.status() == MemberSubmissionStatus.SUBMITTED ? "제출 완료" : "미제출") + ")"));
			body.append("<p class=\"muted\">금주 진행사항 (").append(member.thisWeekEntries().size()).append("건)</p>");
			body.append(entryTable(member.thisWeekEntries()));
			body.append("<p class=\"muted\">차주 진행사항 (").append(member.nextWeekEntries().size()).append("건)</p>");
			body.append(entryTable(member.nextWeekEntries()));
		}

		return document(body.toString());
	}

	// ----- 이메일(Slack-style 카드 요약) -----

	static String buildIndividualCardEmailHtml(WeeklyReportExportView view) {
		WeeklyReportResponse r = view.report();
		StringBuilder card = new StringBuilder();
		card.append(cardOpen(escape(view.authorName()) + " 주간 보고", r.weekStart(), r.weekEnd()));
		card.append("<p class=\"muted\">작성자: ").append(escape(view.authorName())).append(" · 상태: ")
				.append(r.status() == WeeklyReportStatus.SUBMITTED ? "제출 완료" : "작성 중").append("</p>");
		card.append("<div class=\"badges\">")
				.append(badge("금주", r.entries().thisWeek().size()))
				.append(badge("차주", r.entries().nextWeek().size()))
				.append("</div>");
		card.append(cardListSection("금주 진행사항", r.entries().thisWeek().stream().map(ReportExportHtmlBuilder::entryLabel).toList()));
		card.append(cardListSection("차주 진행사항", r.entries().nextWeek().stream().map(ReportExportHtmlBuilder::entryLabel).toList()));
		card.append(cardClose());
		return document(card.toString());
	}

	static String buildTeamCardEmailHtml(TeamWeeklyReportExportView view) {
		TeamDashboardResponse r = view.report();
		StringBuilder card = new StringBuilder();
		card.append(cardOpen("팀 주간 보고", r.weekStart(), r.weekEnd()));
		card.append("<div class=\"badges\">")
				.append(badge("제출", r.submittedCount() + "/" + r.totalMemberCount()))
				.append("</div>");
		card.append("<div class=\"section\"><div class=\"section-title\">멤버별 제출 현황</div><ul>");
		for (TeamMemberReportEntries m : r.members()) {
			card.append("<li>").append(escape(m.name())).append(" - ")
					.append(m.status() == MemberSubmissionStatus.SUBMITTED ? "제출 완료" : "미제출")
					.append(" (금주 ").append(m.thisWeekEntries().size()).append("건, 차주 ")
					.append(m.nextWeekEntries().size()).append("건)</li>");
		}
		card.append("</ul></div>");
		card.append(cardClose());
		return document(card.toString());
	}

	// ----- 공통 조각 -----

	private static String pageHeader(String title, LocalDate weekStart, LocalDate weekEnd) {
		return "<h1>" + title + "</h1><p class=\"period\">" + formatDate(weekStart) + " ~ "
				+ formatDate(weekEnd) + "</p>";
	}

	private static String sectionTitle(String text) {
		return "<h2>" + text + "</h2>";
	}

	private static String cardOpen(String title, LocalDate weekStart, LocalDate weekEnd) {
		return "<div class=\"card\"><div class=\"card-title\">" + title + "</div>"
				+ "<div class=\"card-period\">" + formatDate(weekStart) + " ~ " + formatDate(weekEnd) + "</div>";
	}

	private static String cardClose() {
		return "</div>";
	}

	private static String badge(String label, int count) {
		return badge(label, String.valueOf(count));
	}

	private static String badge(String label, String value) {
		return "<span class=\"badge\">" + escape(label) + " " + escape(value) + "</span>";
	}

	private static String cardListSection(String title, List<String> items) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class=\"section\"><div class=\"section-title\">").append(escape(title))
				.append(" (").append(items.size()).append("건)</div>");
		if (items.isEmpty()) {
			sb.append("<p class=\"muted\">해당 없음</p>");
		} else {
			sb.append("<ul>");
			items.stream().limit(CARD_ITEM_LIMIT).forEach(item -> sb.append("<li>").append(escape(item)).append("</li>"));
			if (items.size() > CARD_ITEM_LIMIT) {
				sb.append("<li class=\"muted\">외 ").append(items.size() - CARD_ITEM_LIMIT).append("건</li>");
			}
			sb.append("</ul>");
		}
		sb.append("</div>");
		return sb.toString();
	}

	private static String entryLabel(EntryResponse entry) {
		return entry.projectName() + " / " + entry.middleCategoryName()
				+ (hasText(entry.minorCategory()) ? " - " + entry.minorCategory() : "")
				+ " (" + entry.ratePercent() + "%)";
	}

	// ----- 표(테이블) -----

	private static String entryTable(List<EntryResponse> items) {
		if (items.isEmpty()) {
			return "<p class=\"muted\">해당 없음</p>";
		}
		StringBuilder sb = new StringBuilder("<table><thead><tr><th>대분류</th><th>중분류</th><th>소분류</th>"
				+ "<th>상세업무</th><th>달성율</th></tr></thead><tbody>");
		for (EntryResponse e : items) {
			sb.append("<tr><td>").append(escape(e.projectName())).append("</td><td>")
					.append(escape(e.middleCategoryName())).append("</td><td>").append(escape(e.minorCategory()))
					.append("</td><td>").append(nl2br(escape(e.detail()))).append("</td><td>")
					.append(e.ratePercent()).append("%</td></tr>");
		}
		return sb.append("</tbody></table>").toString();
	}

	// ----- 문서 골격 -----

	private static String document(String bodyHtml) {
		return "<!DOCTYPE html>"
				+ "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\" />"
				+ "<style>" + css() + "</style></head><body>" + bodyHtml + "</body></html>";
	}

	private static String css() {
		return "@page { size: A4; margin: 18mm 15mm; } "
				+ "body { font-family: '" + ReportPdfRenderer.FONT_FAMILY + "', sans-serif; font-size: 11px; color: #222; } "
				+ "h1 { font-size: 18px; margin: 0 0 4px 0; } "
				+ "h2 { font-size: 13px; margin: 16px 0 6px 0; border-bottom: 1px solid #ccc; padding-bottom: 4px; } "
				+ "p.period, p { margin: 2px 0; } "
				+ "table { width: 100%; border-collapse: collapse; margin-bottom: 8px; } "
				+ "th, td { border: 1px solid #ddd; padding: 4px 6px; text-align: left; font-size: 10px; } "
				+ "th { background-color: #f2f2f2; } "
				+ "p.muted, .muted { color: #888; } "
				+ ".card { border: 1px solid #ddd; border-radius: 6px; padding: 14px; } "
				+ ".card-title { font-size: 16px; font-weight: bold; } "
				+ ".card-period { color: #666; margin-bottom: 8px; } "
				+ ".badges { margin: 8px 0; } "
				+ ".badge { display: inline-block; background-color: #eef2ff; color: #3949ab; border-radius: 10px; "
				+ "padding: 3px 10px; margin-right: 6px; font-size: 10px; } "
				+ ".section { margin-top: 10px; } "
				+ ".section-title { font-weight: bold; margin-bottom: 4px; } "
				+ "ul { margin: 4px 0; padding-left: 18px; }";
	}

	// ----- 서식/이스케이프 -----

	private static String formatDate(LocalDate date) {
		return date != null ? date.format(DATE) : "-";
	}

	private static String formatDateTime(LocalDateTime dateTime) {
		return dateTime != null ? dateTime.format(DATE_TIME) : "-";
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private static String nl2br(String text) {
		return text == null ? "" : text.replace("\n", "<br />");
	}

	/** XML(XHTML) 파서가 깨지지 않도록 사용자 입력(상세업무/소분류 등)을 반드시 이 메서드로 이스케이프한다. */
	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
