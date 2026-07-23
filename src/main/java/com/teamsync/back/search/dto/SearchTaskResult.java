package com.teamsync.back.search.dto;

import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.TaskStatus;

/**
 * FR-004(통합 검색) 태스크 검색 결과 항목. title/description 검색 매칭 대상.
 */
public record SearchTaskResult(
		Long id,
		String title,
		TaskStatus status,
		TaskPriority priority,
		Long projectId,
		String projectName
) {
	public static SearchTaskResult from(Task task) {
		return new SearchTaskResult(
				task.getId(),
				task.getTitle(),
				task.getStatus(),
				task.getPriority(),
				task.getProject().getId(),
				task.getProject().getName());
	}
}
