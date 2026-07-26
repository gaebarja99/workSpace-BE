package com.teamsync.back.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;

/**
 * 이메일 실발송 구현(JavaMailSender 기반). email.enabled=true 이고 SMTP 설정
 * (JavaMailSender 빈)이 있을 때만 활성화된다. SSO의 실/Mock 전환 선례와 동일하게 설정 플래그로 갈아끼운다.
 */
@Slf4j
public class SmtpEmailSender implements EmailSender {

	private final JavaMailSender mailSender;
	private final String from;

	public SmtpEmailSender(JavaMailSender mailSender, String from) {
		this.mailSender = mailSender;
		this.from = from;
	}

	@Override
	public void send(String toEmail, String subject, String body) {
		SimpleMailMessage mail = new SimpleMailMessage();
		if (StringUtils.hasText(from)) {
			mail.setFrom(from);
		}
		mail.setTo(toEmail);
		mail.setSubject(subject);
		mail.setText(body);
		mailSender.send(mail);
		log.debug("[SMTP-EMAIL] sent to={} subject=\"{}\"", toEmail, subject);
	}

	/**
	 * FR-409: 첨부(PDF) 유무와 무관하게 항상 멀티파트 메시지로 구성한다(본문은 HTML). 실패 시
	 * RuntimeException으로 감싸 던지며, best-effort 처리는 상위 호출부(ReportExportService)의
	 * 책임으로 둔다(이메일 발송이 API 응답 본문(sentTo)에 직접 반영되어야 해서 여기서는 삼키지 않는다).
	 */
	@Override
	public void sendHtmlWithAttachment(String toEmail, String subject, String htmlBody, String attachmentFilename,
			byte[] attachmentBytes, String attachmentContentType) {
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
			if (StringUtils.hasText(from)) {
				helper.setFrom(from);
			}
			helper.setTo(toEmail);
			helper.setSubject(subject);
			helper.setText(htmlBody, true);
			if (attachmentBytes != null && attachmentBytes.length > 0) {
				helper.addAttachment(attachmentFilename, new ByteArrayResource(attachmentBytes), attachmentContentType);
			}
			mailSender.send(mimeMessage);
			log.debug("[SMTP-EMAIL] sent(html+attachment) to={} subject=\"{}\"", toEmail, subject);
		} catch (MessagingException e) {
			throw new IllegalStateException("이메일(첨부 포함) 발송에 실패했습니다.", e);
		}
	}

	@Override
	public boolean isMock() {
		return false;
	}
}
