package com.teamsync.back.task.recurrence.dto;

import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.recurrence.RecurrenceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.util.List;

/**
 * FR-106 반복 태스크 템플릿 부분 업데이트(PATCH) 요청. 모든 필드는 nullable이며, null이 아닌
 * 필드만 반영한다. assigneeIds가 명시적으로 빈 리스트([])로 오면 400 VALIDATION_ERROR로 거부한다.
 * recurrenceType을 바꾸지 않고 dayOfWeek/dayOfMonth만 단독으로 보내면 기존 recurrenceType 기준으로
 * 재검증한다(예: 기존 WEEKLY 템플릿에 dayOfMonth만 보내는 것은 허용하지 않음 — 서비스 계층에서 최종 상태를
 * 검증).
 */
public record RecurringTaskTemplateUpdateRequest(
		@Size(max = 200)
		String title,

		String description,

		TaskPriority priority,

		List<Long> assigneeIds,

		RecurrenceType recurrenceType,

		DayOfWeek dayOfWeek,

		@Min(1) @Max(31)
		Integer dayOfMonth,

		@Min(0)
		Integer dueInDays,

		Boolean active
) {
}
