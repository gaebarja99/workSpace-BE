package com.teamsync.back.task.dto;

import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 목록 조회(GET /api/projects/{projectId}/tasks) 응답. 체크리스트는 포함하지 않는 요약형.
 */
public record TaskSummaryResponse(
		Long id,
		Long projectId,
		String title,
		TaskStatus status,
		TaskPriority priority,
		LocalDate startDate,
		LocalDate dueDate,
		List<AssigneeResponse> assignees,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static TaskSummaryResponse from(Task task) {
		return new TaskSummaryResponse(
				task.getId(),
				task.getProject().getId(),
				task.getTitle(),
				task.getStatus(),
				task.getPriority(),
				task.getStartDate(),
				task.getDueDate(),
				task.getAssignees().stream().map(AssigneeResponse::from).toList(),
				task.getCreatedAt(),
				task.getUpdatedAt());
	}
}
