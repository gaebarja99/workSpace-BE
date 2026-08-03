package com.teamsync.back.task.dto;

import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 단건 상세 조회(GET /api/tasks/{taskId}) 응답.
 * recurringTemplateId(FR-106): 이 태스크가 반복 태스크 템플릿의 일 배치로 자동 생성된 경우에만 값이
 * 채워진다(일반 생성 태스크는 null).
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
		Long recurringTemplateId,
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
				task.getRecurringTemplate() != null ? task.getRecurringTemplate().getId() : null,
				task.getCreatedAt(),
				task.getUpdatedAt());
	}
}
