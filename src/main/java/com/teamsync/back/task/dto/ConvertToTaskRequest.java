package com.teamsync.back.task.dto;

import com.teamsync.back.task.TaskPriority;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * FR-301(US-09): 채널 메시지를 태스크로 전환하는 요청.
 * title/description을 비워두면 서비스 계층이 메시지 content로 채운다(title은 앞 80자, description은 전체).
 * assigneeIds는 TaskCreateRequest와 동일하게 최소 1명 이상이어야 한다.
 */
public record ConvertToTaskRequest(
		@Size(max = 200)
		String title,

		String description,

		@NotEmpty(message = "담당자는 최소 1명 이상이어야 합니다.")
		List<Long> assigneeIds,

		LocalDate dueDate,

		TaskPriority priority
) {
}
