package com.teamsync.back.email;

import lombok.extern.slf4j.Slf4j;

/**
 * 이메일 발송기 기본(Mock) 구현. 실제 SMTP 연동 없이 발송 시도를 로그로만 남긴다.
 * email.enabled=false(기본)이거나 JavaMailSender가 없을 때 활성화된다.
 */
@Slf4j
public class LoggingEmailSender implements EmailSender {

	@Override
	public void send(String toEmail, String subject, String body) {
		log.info("[MOCK-EMAIL] to={} subject=\"{}\" body=\"{}\"", toEmail, subject, body);
	}

	@Override
	public void sendHtmlWithAttachment(String toEmail, String subject, String htmlBody, String attachmentFilename,
			byte[] attachmentBytes, String attachmentContentType) {
		int attachmentSize = attachmentBytes != null ? attachmentBytes.length : 0;
		log.info("[MOCK-EMAIL] to={} subject=\"{}\" attachment={}({} bytes) htmlBodyLength={}", toEmail, subject,
				attachmentFilename, attachmentSize, htmlBody != null ? htmlBody.length() : 0);
	}

	@Override
	public boolean isMock() {
		return true;
	}
}
