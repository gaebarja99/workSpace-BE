package com.teamsync.back.notification;

/**
 * FR-003 알림 세분화 설정의 카테고리(고정 5종). GET /api/notifications/preferences가 이 선언 순서대로
 * 노출하며, 각 카테고리는 채널별(인앱/이메일/푸시) 기본값 매트릭스를 함께 갖는다(계약 v1).
 *
 * NotificationType -> NotificationCategory 매핑도 이 한 곳({@link #of(NotificationType)})에 두어
 * 설정 조회(API)와 발송 훅(NotificationService)이 같은 규칙을 재사용하게 한다.
 */
public enum NotificationCategory {

	// (defaultInApp, defaultEmail, defaultPush) — 계약 "기본값 매트릭스" 그대로.
	TASK_DEADLINE(true, true, false),
	MENTION(true, true, true),
	TASK_ASSIGNED(true, false, false),
	TASK_STATUS_CHANGED(true, false, false),
	WEEKLY_REPORT(true, true, false);

	private final boolean defaultInApp;
	private final boolean defaultEmail;
	private final boolean defaultPush;

	NotificationCategory(boolean defaultInApp, boolean defaultEmail, boolean defaultPush) {
		this.defaultInApp = defaultInApp;
		this.defaultEmail = defaultEmail;
		this.defaultPush = defaultPush;
	}

	public boolean defaultInApp() {
		return defaultInApp;
	}

	public boolean defaultEmail() {
		return defaultEmail;
	}

	public boolean defaultPush() {
		return defaultPush;
	}

	/** 저장된 설정이 없을 때 응답/발송에 사용할 기본 채널 조합. */
	public EffectiveChannels defaults() {
		return new EffectiveChannels(defaultInApp, defaultEmail, defaultPush);
	}

	/**
	 * 발송 시 알림 종류(NotificationType)가 어느 설정 카테고리에 속하는지 매핑한다(계약 표 그대로).
	 * 새 NotificationType이 생기면 여기서 컴파일 에러가 나도록 switch를 exhaustive하게 유지한다.
	 */
	public static NotificationCategory of(NotificationType type) {
		return switch (type) {
			case TASK_DUE_SOON, TASK_DUE_TODAY -> TASK_DEADLINE;
			case MENTION -> MENTION;
			case TASK_ASSIGNED -> TASK_ASSIGNED;
			case TASK_STATUS_CHANGED -> TASK_STATUS_CHANGED;
			case WEEKLY_REPORT_REMINDER -> WEEKLY_REPORT;
		};
	}
}
