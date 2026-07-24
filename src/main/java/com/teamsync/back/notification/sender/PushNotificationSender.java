package com.teamsync.back.notification.sender;

import com.teamsync.back.user.User;

/**
 * FR-003 모바일 푸시 발송기. FCM 등 실제 연동에 필요한 자격증명이 배포 env에 없으므로 현재는 로그 기반
 * Mock({@link LoggingPushNotificationSender})만 제공한다. notification.push.enabled 플래그로 향후
 * 실발송 전환 지점을 표시해둔다(현재는 플래그와 무관하게 로그만 남김).
 */
public interface PushNotificationSender {

	void send(User recipient, String title, String body);
}
