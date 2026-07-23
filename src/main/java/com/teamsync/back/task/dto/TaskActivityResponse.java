package com.teamsync.back.task.dto;

import com.teamsync.back.task.TaskActivity;
import com.teamsync.back.task.TaskActivityType;
import java.time.LocalDateTime;

/**
 * FR-105-B(태스크 활동 로그) 응답. actor가 null이면 시스템/자동 발생 활동이므로 actorName은 "시스템"으로 내린다.
 */
public record TaskActivityResponse(
		Long id,
		String actorName,
		TaskActivityType activityType,
		String detail,
		LocalDateTime createdAt
) {
	public static TaskActivityResponse from(TaskActivity activity) {
		return new TaskActivityResponse(
				activity.getId(),
				activity.getActor() != null ? activity.getActor().getName() : "시스템",
				activity.getActivityType(),
				activity.getDetail(),
				activity.getCreatedAt());
	}
}
