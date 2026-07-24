package com.teamsync.back.notification.sender;

import com.teamsync.back.user.User;
import lombok.extern.slf4j.Slf4j;

/**
 * FR-003 푸시 발송기 기본(Mock) 구현. FCM 자격증명 부재로 실발송은 미구현이며 발송 시도를 로그로 남긴다.
 * notification.push.enabled 값을 함께 남겨 향후 실발송 전환 시점의 참고 지표로 삼는다.
 */
@Slf4j
public class LoggingPushNotificationSender implements PushNotificationSender {

	private final boolean enabled;

	public LoggingPushNotificationSender(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void send(User recipient, String title, String body) {
		log.info("[MOCK-PUSH enabled={}] to={} title=\"{}\" body=\"{}\"", enabled, recipient.getEmail(), title, body);
	}
}
