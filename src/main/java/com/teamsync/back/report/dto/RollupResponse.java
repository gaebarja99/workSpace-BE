package com.teamsync.back.report.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/reports/rollup(FR-407, 임원 조직 롤업 대시보드) 응답. "조직"은 로그인한 사용자의
 * 워크스페이스로 축소한 개념이다(계약 문서: 멀티워크스페이스 임원 개념 없음). teams는 워크스페이스의
 * ACTIVE 프로젝트("팀")를 projectId 오름차순으로 나열하고, trend는 weekStart를 포함한 최근 4주
 * (과거 3주 역산)의 조직 전체 완료율 추이다.
 */
public record RollupResponse(
		LocalDate weekStart,
		LocalDate weekEnd,
		int workspaceMemberCount,
		List<RollupTeamItem> teams,
		int orgCompletionRate,
		List<RollupTrendItem> trend
) {
}
