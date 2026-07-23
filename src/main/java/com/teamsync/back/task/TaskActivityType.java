package com.teamsync.back.task;

/**
 * FR-105-B(태스크 활동 로그): 태스크에 대한 활동 이력 종류. task_activities.activity_type CHECK 제약과
 * 반드시 일치해야 한다(값 추가 시 V11 이후 마이그레이션에서 CHECK도 함께 갱신).
 */
public enum TaskActivityType {
	CREATED,
	STATUS_CHANGED,
	ASSIGNEE_CHANGED,
	COMMENT_ADDED
}
