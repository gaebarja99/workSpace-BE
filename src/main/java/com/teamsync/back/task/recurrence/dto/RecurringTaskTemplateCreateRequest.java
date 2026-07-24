package com.teamsync.back.task.recurrence.dto;

import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.recurrence.RecurrenceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.util.List;

/**
 * FR-106 반복 태스크 템플릿 생성 요청.
 * priority/active는 미지정 시 서비스 계층에서 기본값(MEDIUM/true)을 적용한다.
 * recurrenceType=WEEKLY면 dayOfWeek 필수·dayOfMonth는 반드시 null, MONTHLY면 그 반대다(서비스 검증, 400).
 */
public record RecurringTaskTemplateCreateRequest(
		@NotBlank(message = "템플릿 제목은 필수입니다.")
		@Size(max = 200)
		String title,

		String description,

		TaskPriority priority,

		@NotEmpty(message = "담당자는 최소 1명 이상이어야 합니다.")
		List<Long> assigneeIds,

		@NotNull(message = "반복 주기(recurrenceType)는 필수입니다.")
		RecurrenceType recurrenceType,

		DayOfWeek dayOfWeek,

		@Min(1) @Max(31)
		Integer dayOfMonth,

		@NotNull(message = "dueInDays는 필수입니다.")
		@Min(0)
		Integer dueInDays,

		Boolean active
) {
}
