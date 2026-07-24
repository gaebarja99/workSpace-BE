package com.teamsync.back.notification.sender;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FR-003 이메일/푸시 발송기 설정. BackApplication의 @ConfigurationPropertiesScan으로 자동 등록된다
 * (SsoProperties와 동일 컨벤션).
 *
 * 배포 env에 자격증명이 없는 경우를 기본으로 가정해 email/push 모두 기본 비활성(로그 Mock)이며,
 * env(NOTIFICATION_EMAIL_ENABLED 등)로 실발송을 켠다.
 */
@ConfigurationProperties(prefix = "notification")
public record NotificationSenderProperties(
		Email email,
		Push push
) {

	public record Email(boolean enabled, String from) {
	}

	public record Push(boolean enabled) {
	}

	/** yml에서 notification.email 블록 자체가 비어도 NPE 없이 기본값(비활성)으로 동작하게 보정. */
	public NotificationSenderProperties {
		if (email == null) {
			email = new Email(false, null);
		}
		if (push == null) {
			push = new Push(false);
		}
	}
}
