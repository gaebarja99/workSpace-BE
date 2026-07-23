package com.teamsync.back.task.dto;

import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * FR-102 태스크 생성 요청.
 * priority/status는 미지정 시 서비스 계층에서 기본값(MEDIUM/TODO)을 적용한다.
 * assigneeIds는 최소 1명 이상이어야 한다(비어있으면 400 VALIDATION_ERROR).
 */
public record TaskCreateRequest(
		@NotBlank(message = "태스크 제목은 필수입니다.")
		@Size(max = 200)
		String title,

		String description,

		TaskPriority priority,

		TaskStatus status,

		LocalDate startDate,

		LocalDate dueDate,

		@NotEmpty(message = "담당자는 최소 1명 이상이어야 합니다.")
		List<Long> assigneeIds
) {
}
