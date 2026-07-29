package com.teamsync.back.report.dto;

import java.util.List;

/** GET /api/reports/executive 응답의 프로젝트(대분류) 그룹. 해당 프로젝트에 entry가 있는 멤버만 포함한다. */
public record ExecutiveCategoryGroup(
		Long projectId,
		String projectName,
		List<ExecutiveMemberEntries> members
) {
}
