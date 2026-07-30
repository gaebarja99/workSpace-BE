package com.teamsync.back.search.dto;

import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectStatus;

/**
 * FR-004(통합 검색) 프로젝트 검색 결과 항목. name 검색 매칭 대상.
 */
public record SearchProjectResult(
		Long id,
		String name,
		ProjectStatus status
) {
	public static SearchProjectResult from(Project project) {
		return new SearchProjectResult(project.getId(), project.getName(), project.getStatus());
	}
}
