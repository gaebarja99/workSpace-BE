package com.teamsync.back.task.dto;

import jakarta.validation.constraints.Size;

/**
 * content/isChecked 모두 nullable이며, null이 아닌 필드만 반영한다.
 */
public record ChecklistItemUpdateRequest(
		@Size(max = 500)
		String content,

		Boolean isChecked
) {
}
