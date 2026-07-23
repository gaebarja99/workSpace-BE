package com.teamsync.back.task.dto;

import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FR-104(담당자별 대시보드, US-01 "내 업무") 응답.
 * GET /api/tasks/me 전용으로, TaskSummaryResponse와 동일한 필드에 projectName을 추가한 형태다.
 */
public record MyTaskResponse(
		Long id,
		Long projectId,
		String projectName,
		String title,
		TaskStatus status,
		TaskPriority priority,
		LocalDate startDate,
		LocalDate dueDate,
		List<AssigneeResponse> assignees,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static MyTaskResponse from(Task task) {
		return new MyTaskResponse(
				task.getId(),
				task.getProject().getId(),
				task.getProject().getName(),
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
