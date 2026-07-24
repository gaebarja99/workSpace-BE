package com.teamsync.back.task.recurrence.dto;

import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.recurrence.RecurrenceType;
import com.teamsync.back.task.recurrence.RecurringTaskTemplate;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RecurringTaskTemplateResponse(
		Long id,
		Long projectId,
		String title,
		String description,
		TaskPriority priority,
		List<TemplateMemberResponse> assignees,
		RecurrenceType recurrenceType,
		DayOfWeek dayOfWeek,
		Integer dayOfMonth,
		Integer dueInDays,
		boolean active,
		LocalDate lastGeneratedAt,
		TemplateMemberResponse createdBy,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	public static RecurringTaskTemplateResponse from(RecurringTaskTemplate template) {
		return new RecurringTaskTemplateResponse(
				template.getId(),
				template.getProject().getId(),
				template.getTitle(),
				template.getDescription(),
				template.getPriority(),
				template.getAssignees().stream().map(TemplateMemberResponse::from).toList(),
				template.getRecurrenceType(),
				template.getDayOfWeek(),
				template.getDayOfMonth(),
				template.getDueInDays(),
				template.isActive(),
				template.getLastGeneratedAt(),
				template.getCreatedBy() != null ? TemplateMemberResponse.from(template.getCreatedBy()) : null,
				template.getCreatedAt(),
				template.getUpdatedAt());
	}
}
