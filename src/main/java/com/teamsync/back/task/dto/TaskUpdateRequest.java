package com.teamsync.back.task.dto;

import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.TaskStatus;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * FR-102 태스크 부분 업데이트(PATCH) 요청. 모든 필드는 nullable이며, null이 아닌 필드만 반영한다.
 * assigneeIds가 null이면 담당자를 변경하지 않고, 명시적으로 빈 리스트([])가 오면 400 VALIDATION_ERROR로 거부한다
 * (담당자는 항상 최소 1명 이상이어야 하므로).
 */
public record TaskUpdateRequest(
		@Size(max = 200)
		String title,

		String description,

		TaskPriority priority,

		TaskStatus status,

		LocalDate startDate,

		LocalDate dueDate,

		List<Long> assigneeIds,

		// FR-302(US-09): 태스크 생성/상태변경/완료 시 채널 시스템 메시지 자동 게시 on/off 토글. null이면 변경하지 않는다.
		Boolean channelNotificationsEnabled
) {
}
