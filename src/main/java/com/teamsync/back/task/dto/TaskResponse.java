package com.teamsync.back.task.dto;

import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 단건 상세 조회(GET /api/tasks/{taskId}) 응답. 체크리스트를 포함한다.
 */
public record TaskResponse(
		Long id,
		Long projectId,
		String title,
		String description,
		TaskStatus status,
		TaskPriority priority,
		LocalDate startDate,
		LocalDate dueDate,
		List<AssigneeResponse> assignees,
		List<ChecklistItemResponse> checklistItems,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static TaskResponse from(Task task) {
		return new TaskResponse(
				task.getId(),
				task.getProject().getId(),
				task.getTitle(),
				task.getDescription(),
				task.getStatus(),
				task.getPriority(),
				task.getStartDate(),
				task.getDueDate(),
				task.getAssignees().stream().map(AssigneeResponse::from).toList(),
				task.getChecklistItems().stream().map(ChecklistItemResponse::from).toList(),
				task.getCreatedAt(),
				task.getUpdatedAt());
	}
}
