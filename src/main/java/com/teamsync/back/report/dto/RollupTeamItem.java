package com.teamsync.back.report.dto;

/**
 * GET /api/reports/rollup(FR-407)의 팀(=Project, ACTIVE만) 단위 요약 1건. memberCount는 프로젝트별
 * 멤버십 테이블이 없어 워크스페이스 전체 User(GUEST 제외) 수로 근사한다(기존 프로젝트 관리 근사 방식과
 * 동일 원칙, 계약 문서 명시). submittedCount는 WeeklyReport.status=SUBMITTED distinct user 수(실데이터).
 * completionRate/overdueRate는 완료+진행+이슈 태스크 수를 분모로 한 0~100 정수 반올림 %이며, 분모가 0이면
 * 둘 다 0이다.
 */
public record RollupTeamItem(
		Long projectId,
		String projectName,
		int memberCount,
		int submittedCount,
		int completionRate,
		int overdueRate
) {
}
