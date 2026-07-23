package com.teamsync.back.report.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GET/POST /reports/team(publish) 응답의 멤버 1인 요약. completedTitles/inProgressTitles/issueTitles는
 * 상위 5개 제목만 포함한다(계약 문서: 멤버 카드 펼침 미리보기 용도).
 */
public record TeamMemberReportSummary(
		Long userId,
		String name,
		MemberSubmissionStatus status,
		LocalDateTime submittedAt,
		int completedCount,
		int inProgressCount,
		int issueCount,
		List<String> completedTitles,
		List<String> inProgressTitles,
		List<String> issueTitles,
		String nextWeekPlan
) {
}
