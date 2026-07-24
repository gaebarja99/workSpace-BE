package com.teamsync.back.notification.sender;

import com.teamsync.back.user.User;
import lombok.extern.slf4j.Slf4j;

/**
 * FR-003 이메일 발송기 기본(Mock) 구현. 실제 SMTP 연동 없이 발송 시도를 로그로만 남긴다.
 * notification.email.enabled=false(기본)이거나 JavaMailSender가 없을 때 활성화된다.
 */
@Slf4j
public class LoggingEmailNotificationSender implements EmailNotificationSender {

	@Override
	public void send(User recipient, String subject, String body) {
		log.info("[MOCK-EMAIL] to={} subject=\"{}\" body=\"{}\"", recipient.getEmail(), subject, body);
	}
}
