package com.teamsync.back.notification;

/**
 * FR-003 알림 세분화 설정의 카테고리(FR-406 추가로 6종). GET /api/notifications/preferences가 이
 * 선언 순서대로 노출하며, 각 카테고리는 채널별(인앱/이메일/푸시) 기본값 매트릭스를 함께 갖는다(계약 v1).
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
	WEEKLY_REPORT(true, true, false),
	// FR-406(이슈/리스크 자동 플래그): 기존 5종 중 어느 것도 "이미 발생한 이슈(마감초과/정체/차단)에 대한
	// 운영 경보"라는 성격과 정확히 맞지 않아(TASK_DEADLINE은 "다가올" 마감 임박 알림, TASK_STATUS_CHANGED는
	// 본인 담당 태스크의 단순 상태변경 알림) 새 카테고리를 신설한다. WEEKLY_REPORT와 같은 운영 리포트성
	// 알림으로 보고 기본값도 동일하게(인앱+이메일, 푸시 제외) 맞춘다.
	TASK_ISSUE(true, true, false);

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
			case TASK_ISSUE_FLAGGED -> TASK_ISSUE;
		};
	}
}
