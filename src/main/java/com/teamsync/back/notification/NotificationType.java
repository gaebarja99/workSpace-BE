package com.teamsync.back.notification;

/**
 * FR-108(알림 트리거, US-04)에서 발생 가능한 알림 종류.
 */
public enum NotificationType {
	TASK_DUE_SOON,
	TASK_DUE_TODAY,
	TASK_ASSIGNED,
	TASK_STATUS_CHANGED
}
