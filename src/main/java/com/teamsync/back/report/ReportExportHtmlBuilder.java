package com.teamsync.back.report;

import com.teamsync.back.report.dto.CompletedTaskItem;
import com.teamsync.back.report.dto.InProgressTaskItem;
import com.teamsync.back.report.dto.IssueItem;
import com.teamsync.back.report.dto.MemberSubmissionStatus;
import com.teamsync.back.report.dto.TeamMemberReportSummary;
import com.teamsync.back.report.dto.TeamWeeklyReportExportView;
import com.teamsync.back.report.dto.TeamWeeklyReportResponse;
import com.teamsync.back.report.dto.WeeklyReportExportView;
import com.teamsync.back.report.dto.WeeklyReportResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * FR-409(보고서 내보내기): PDF/이메일 본문용 XHTML 문자열을 만드는 순수 빌더(상태 없음, 정적
 * 메서드만, ReportPdfRenderer/ReportExportService 전용 package-private 유틸). openhtmltopdf는
 * XML 파서로 문서를 읽으므로 모든 태그가 XHTML로 well-formed해야 한다(닫히지 않은 태그, 잘못된
 * 중첩 금지) — 사용자 입력(태스크 제목/메시지 내용 등)은 반드시 {@link #escape}를 거친다.
 *
 * PDF용은 섹션별 전체 목록을 표로 담고, 이메일용은 PRD 문구의 "Slack-style 카드"를 흡수해
 * 제목/기간 + 섹션별 건수 배지 + 상위 5개 항목만 담은 축약 카드로 구성한다(WeeklyReportService의
 * TOP_TITLES_LIMIT=5 관례와 동일한 값을 여기서도 사용).
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
		body.append(pageHeader(view.projectName() + " 주간 보고", r.weekStart(), r.weekEnd()));
		body.append("<p>작성자: ").append(escape(view.authorName())).append(" (")
				.append(escape(view.authorEmail())).append(")</p>");
		body.append("<p>상태: ").append(r.status() == WeeklyReportStatus.SUBMITTED ? "제출 완료" : "작성 중")
				.append(" · 제출일시: ").append(formatDateTime(r.submittedAt()))
				.append(" · 마지막 저장: ").append(formatDateTime(r.lastSavedAt())).append("</p>");

		body.append(sectionTitle("완료한 일 (" + r.completedTasks().size() + "건)"));
		body.append(completedTable(r.completedTasks()));

		body.append(sectionTitle("진행 중인 일 (" + r.inProgressTasks().size() + "건)"));
		body.append(inProgressTable(r.inProgressTasks()));

		body.append(sectionTitle("이슈 (" + r.issues().size() + "건)"));
		body.append(issueTable(r.issues()));

		body.append(sectionTitle("다음 주 계획"));
		body.append("<p>").append(nl2br(escape(r.nextWeekPlan()))).append("</p>");

		return document(body.toString());
	}

	static String buildTeamPdfHtml(TeamWeeklyReportExportView view) {
		TeamWeeklyReportResponse r = view.report();
		StringBuilder body = new StringBuilder();
		body.append(pageHeader(view.projectName() + " 팀 주간 보고", r.weekStart(), r.weekEnd()));
		body.append("<p>").append(publishStateText(r)).append("</p>");
		body.append("<p>제출 현황: ").append(r.submittedCount()).append(" / ").append(r.totalMemberCount())
				.append("명 · 팀 완료 ").append(r.teamCompletedCount()).append("건 · 팀 이슈 ")
				.append(r.teamIssueCount()).append("건</p>");

		body.append(sectionTitle("멤버별 요약 (" + r.members().size() + "명)"));
		body.append(memberTable(r.members()));

		return document(body.toString());
	}

	// ----- 이메일(Slack-style 카드 요약) -----

	static String buildIndividualCardEmailHtml(WeeklyReportExportView view) {
		WeeklyReportResponse r = view.report();
		StringBuilder card = new StringBuilder();
		card.append(cardOpen(view.projectName() + " 주간 보고", r.weekStart(), r.weekEnd()));
		card.append("<p class=\"muted\">작성자: ").append(escape(view.authorName())).append(" · 상태: ")
				.append(r.status() == WeeklyReportStatus.SUBMITTED ? "제출 완료" : "작성 중").append("</p>");
		card.append("<div class=\"badges\">")
				.append(badge("완료", r.completedTasks().size()))
				.append(badge("진행 중", r.inProgressTasks().size()))
				.append(badge("이슈", r.issues().size()))
				.append("</div>");
		card.append(cardListSection("완료한 일", r.completedTasks().stream().map(CompletedTaskItem::title).toList()));
		card.append(cardListSection("진행 중인 일", r.inProgressTasks().stream().map(InProgressTaskItem::title).toList()));
		card.append(cardListSection("이슈", r.issues().stream().map(ReportExportHtmlBuilder::issueLabel).toList()));
		card.append(cardClose());
		return document(card.toString());
	}

	static String buildTeamCardEmailHtml(TeamWeeklyReportExportView view) {
		TeamWeeklyReportResponse r = view.report();
		StringBuilder card = new StringBuilder();
		card.append(cardOpen(view.projectName() + " 팀 주간 보고", r.weekStart(), r.weekEnd()));
		card.append("<p class=\"muted\">").append(publishStateText(r)).append("</p>");
		card.append("<div class=\"badges\">")
				.append(badge("제출", r.submittedCount() + "/" + r.totalMemberCount()))
				.append(badge("팀 완료", String.valueOf(r.teamCompletedCount())))
				.append(badge("팀 이슈", String.valueOf(r.teamIssueCount())))
				.append("</div>");
		card.append("<div class=\"section\"><div class=\"section-title\">멤버별 제출 현황</div><ul>");
		for (TeamMemberReportSummary m : r.members()) {
			card.append("<li>").append(escape(m.name())).append(" - ")
					.append(m.status() == MemberSubmissionStatus.SUBMITTED ? "제출 완료" : "미제출")
					.append(" (완료 ").append(m.completedCount()).append(", 진행 ").append(m.inProgressCount())
					.append(", 이슈 ").append(m.issueCount()).append(")</li>");
		}
		card.append("</ul></div>");
		card.append(cardClose());
		return document(card.toString());
	}

	// ----- 공통 조각 -----

	private static String publishStateText(TeamWeeklyReportResponse r) {
		return r.publishedAt() != null
				? "발행 완료 (발행자: " + escape(nullToDash(r.publishedByName())) + ", 발행일시: "
						+ formatDateTime(r.publishedAt()) + ")"
				: "집계 중(아직 발행되지 않음)";
	}

	private static String pageHeader(String title, LocalDate weekStart, LocalDate weekEnd) {
		return "<h1>" + escape(title) + "</h1><p class=\"period\">" + formatDate(weekStart) + " ~ "
				+ formatDate(weekEnd) + "</p>";
	}

	private static String sectionTitle(String text) {
		return "<h2>" + escape(text) + "</h2>";
	}

	private static String cardOpen(String title, LocalDate weekStart, LocalDate weekEnd) {
		return "<div class=\"card\"><div class=\"card-title\">" + escape(title) + "</div>"
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

	private static String issueLabel(IssueItem issue) {
		String suffix = issue.kind() == com.teamsync.back.report.dto.IssueKind.OVERDUE
				? "마감초과 " + (issue.daysOverdue() != null ? issue.daysOverdue() : 0) + "일"
				: "정체(마지막 업데이트 " + formatDate(issue.staleSinceDate()) + ")";
		return issue.title() + " - " + suffix;
	}

	// ----- 표(테이블) -----

	private static String completedTable(List<CompletedTaskItem> items) {
		if (items.isEmpty()) {
			return "<p class=\"muted\">해당 없음</p>";
		}
		StringBuilder sb = new StringBuilder("<table><thead><tr><th>제목</th><th>마감일</th><th>완료일시</th></tr></thead><tbody>");
		for (CompletedTaskItem t : items) {
			sb.append("<tr><td>").append(escape(t.title())).append("</td><td>").append(formatDate(t.dueDate()))
					.append("</td><td>").append(formatDateTime(t.completedAt())).append("</td></tr>");
		}
		return sb.append("</tbody></table>").toString();
	}

	private static String inProgressTable(List<InProgressTaskItem> items) {
		if (items.isEmpty()) {
			return "<p class=\"muted\">해당 없음</p>";
		}
		StringBuilder sb = new StringBuilder(
				"<table><thead><tr><th>제목</th><th>상태</th><th>우선순위</th><th>마감일</th></tr></thead><tbody>");
		for (InProgressTaskItem t : items) {
			sb.append("<tr><td>").append(escape(t.title())).append("</td><td>").append(t.status())
					.append("</td><td>").append(t.priority()).append("</td><td>").append(formatDate(t.dueDate()))
					.append("</td></tr>");
		}
		return sb.append("</tbody></table>").toString();
	}

	private static String issueTable(List<IssueItem> items) {
		if (items.isEmpty()) {
			return "<p class=\"muted\">해당 없음</p>";
		}
		StringBuilder sb = new StringBuilder(
				"<table><thead><tr><th>제목</th><th>종류</th><th>상세</th></tr></thead><tbody>");
		for (IssueItem i : items) {
			String detail = i.kind() == com.teamsync.back.report.dto.IssueKind.OVERDUE
					? "마감일 " + formatDate(i.dueDate()) + " (" + (i.daysOverdue() != null ? i.daysOverdue() : 0) + "일 지남)"
					: "마지막 업데이트 " + formatDate(i.staleSinceDate());
			sb.append("<tr><td>").append(escape(i.title())).append("</td><td>")
					.append(i.kind() == com.teamsync.back.report.dto.IssueKind.OVERDUE ? "마감 초과" : "장기 정체")
					.append("</td><td>").append(detail).append("</td></tr>");
		}
		return sb.append("</tbody></table>").toString();
	}

	private static String memberTable(List<TeamMemberReportSummary> members) {
		if (members.isEmpty()) {
			return "<p class=\"muted\">해당 없음</p>";
		}
		StringBuilder sb = new StringBuilder("<table><thead><tr><th>이름</th><th>제출 상태</th><th>완료</th><th>진행</th>"
				+ "<th>이슈</th><th>다음 주 계획</th></tr></thead><tbody>");
		for (TeamMemberReportSummary m : members) {
			sb.append("<tr><td>").append(escape(m.name())).append("</td><td>")
					.append(m.status() == MemberSubmissionStatus.SUBMITTED ? "제출 완료" : "미제출").append("</td><td>")
					.append(m.completedCount()).append("</td><td>").append(m.inProgressCount()).append("</td><td>")
					.append(m.issueCount()).append("</td><td>").append(nl2br(escape(m.nextWeekPlan())))
					.append("</td></tr>");
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

	private static String nullToDash(String value) {
		return value != null ? value : "-";
	}

	private static String nl2br(String text) {
		return text == null ? "" : text.replace("\n", "<br />");
	}

	/** XML(XHTML) 파서가 깨지지 않도록 사용자 입력(제목/내용 등)을 반드시 이 메서드로 이스케이프한다. */
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
