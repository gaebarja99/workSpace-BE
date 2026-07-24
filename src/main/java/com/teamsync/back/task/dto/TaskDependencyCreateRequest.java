package com.teamsync.back.task.dto;

import jakarta.validation.constraints.NotNull;

/**
 * FR-107: POST /api/tasks/{taskId}/dependencies 요청. 이 taskId가 successor, predecessorTaskId가
 * predecessor가 되는 관계를 생성한다.
 */
public record TaskDependencyCreateRequest(
		@NotNull(message = "선행 태스크 ID는 필수입니다.")
		Long predecessorTaskId
) {
}
