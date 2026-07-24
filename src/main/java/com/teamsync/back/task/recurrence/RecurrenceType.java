package com.teamsync.back.task.recurrence;

/**
 * FR-106: 반복 태스크 템플릿의 반복 주기.
 * WEEKLY는 dayOfWeek(요일)를, MONTHLY는 dayOfMonth(1~31)를 함께 요구한다(서비스 레벨 상호배타 검증).
 */
public enum RecurrenceType {
	WEEKLY,
	MONTHLY
}
