package com.teamsync.back.report.dto;

import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.TaskStatus;
import java.time.LocalDate;

/**
 * FR-401 "진행 중인 일" 섹션 1건. isNew는 해당 태스크가 이 주차([weekStart, weekEnd+1일) 범위)에
 * 새로 생성되었는지 여부(createdAt 기준)다.
 */
public record InProgressTaskItem(
		Long taskId,
		String title,
		TaskStatus status,
		TaskPriority priority,
		LocalDate dueDate,
		boolean isNew
) {
}
