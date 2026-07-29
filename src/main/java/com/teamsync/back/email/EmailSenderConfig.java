package com.teamsync.back.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 이메일 발송기 빈 구성. 설정 플래그로 실발송/로그 Mock을 갈아끼운다.
 *
 * email.enabled=true 이고 JavaMailSender(spring.mail.* 설정)가 있으면 SMTP 실발송, 그 외에는 로그 Mock.
 * JavaMailSender는 스프링 부트 자동설정이 등록하는 빈이라, 사용자 @Configuration에서 @ConditionalOnBean으로
 * 참조하면 평가 순서상 아직 미등록으로 취급되어 조건이 항상 false가 된다(부트가 경고하는 안티패턴). 따라서
 * ObjectProvider로 "빈 생성 시점(=모든 빈 정의 등록 이후)"에 지연 조회해 실제 존재 여부로 분기한다.
 */
@Slf4j
@Configuration
public class EmailSenderConfig {

	@Bean
	@ConditionalOnMissingBean(EmailSender.class)
	public EmailSender emailSender(EmailSenderProperties properties, ObjectProvider<JavaMailSender> mailSenderProvider) {
		JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
		if (properties.enabled() && mailSender != null) {
			log.info("이메일 발송: SMTP 실발송(SmtpEmailSender) 활성화.");
			return new SmtpEmailSender(mailSender, properties.from());
		}
		if (properties.enabled()) {
			// enabled=true 지만 JavaMailSender가 없음(spring.mail 설정 부재) → 실발송 불가, 로그 Mock으로 폴백.
			log.warn("이메일 발송: email.enabled=true 이지만 SMTP(JavaMailSender) 설정이 없어 "
					+ "로그 Mock으로 동작합니다. spring.mail.host 등을 설정하세요.");
		} else {
			log.info("이메일 발송: 비활성(email.enabled=false) → 로그 Mock으로 동작합니다.");
		}
		return new LoggingEmailSender();
	}
}
