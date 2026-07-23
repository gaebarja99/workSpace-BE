package com.teamsync.back.task.dto;

import com.teamsync.back.task.TaskChecklistItem;

public record ChecklistItemResponse(
		Long id,
		String content,
		boolean isChecked
) {
	public static ChecklistItemResponse from(TaskChecklistItem item) {
		return new ChecklistItemResponse(item.getId(), item.getContent(), item.isChecked());
	}
}
