package com.teamsync.back.notification.sender;

import com.teamsync.back.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.StringUtils;

/**
 * FR-003 이메일 실발송 구현(JavaMailSender 기반). notification.email.enabled=true 이고 SMTP 설정
 * (JavaMailSender 빈)이 있을 때만 활성화된다. SSO의 실/Mock 전환 선례와 동일하게 설정 플래그로 갈아끼운다.
 * 발송 실패는 상위(NotificationService)에서 best-effort로 삼켜지므로 여기서는 예외를 그대로 전파한다.
 */
@Slf4j
public class SmtpEmailNotificationSender implements EmailNotificationSender {

	private final JavaMailSender mailSender;
	private final String from;

	public SmtpEmailNotificationSender(JavaMailSender mailSender, String from) {
		this.mailSender = mailSender;
		this.from = from;
	}

	@Override
	public void send(User recipient, String subject, String body) {
		SimpleMailMessage mail = new SimpleMailMessage();
		if (StringUtils.hasText(from)) {
			mail.setFrom(from);
		}
		mail.setTo(recipient.getEmail());
		mail.setSubject(subject);
		mail.setText(body);
		mailSender.send(mail);
		log.debug("[SMTP-EMAIL] sent to={} subject=\"{}\"", recipient.getEmail(), subject);
	}
}
