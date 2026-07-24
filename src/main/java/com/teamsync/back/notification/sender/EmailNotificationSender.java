package com.teamsync.back.notification.sender;

import com.teamsync.back.user.User;

/**
 * FR-003 이메일 발송기. 기본 구현은 로그 기반 Mock({@link LoggingEmailNotificationSender})이며,
 * notification.email.enabled=true & SMTP(JavaMailSender) 설정이 있으면 실발송({@link SmtpEmailNotificationSender})으로 대체된다.
 * 발송은 best-effort로, 호출부(NotificationService)에서 예외를 삼켜 알림/태스크 트랜잭션을 롤백시키지 않는다.
 */
public interface EmailNotificationSender {

	void send(User recipient, String subject, String body);
}
