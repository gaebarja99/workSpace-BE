package com.teamsync.back.report.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * FR-401 "완료한 일" 섹션 1건. completedAt은 Task.updatedAt을 그대로 쓴다(완료 시점을 별도 컬럼으로
 * 남기지 않으므로 상태를 DONE으로 바꾼 마지막 저장 시각이 곧 완료 시각이라는 근사, 계약 문서 기준).
 */
public record CompletedTaskItem(
		Long taskId,
		String title,
		LocalDate dueDate,
		LocalDateTime completedAt
) {
}
