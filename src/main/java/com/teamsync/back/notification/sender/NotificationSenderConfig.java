package com.teamsync.back.notification.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * FR-003 발송기 빈 구성. SSO의 실/Mock 전환 선례를 따라 설정 플래그로 실발송/로그 Mock을 갈아끼운다.
 *
 * - 이메일: notification.email.enabled=true 이고 JavaMailSender(spring.mail.* 설정)가 있으면 SMTP 실발송,
 *   그 외에는 로그 Mock. JavaMailSender는 스프링 부트 자동설정이 등록하는 빈이라, 사용자 @Configuration에서
 *   @ConditionalOnBean으로 참조하면 평가 순서상 아직 미등록으로 취급되어 조건이 항상 false가 된다(부트가
 *   경고하는 안티패턴). 따라서 ObjectProvider로 "빈 생성 시점(=모든 빈 정의 등록 이후)"에 지연 조회해
 *   실제 존재 여부로 분기한다.
 * - 푸시: 실제 FCM 연동 자격증명이 없어 항상 로그 Mock. enabled 값은 로그에 남겨 전환 지점만 표시한다.
 */
@Slf4j
@Configuration
public class NotificationSenderConfig {

	@Bean
	@ConditionalOnMissingBean(EmailNotificationSender.class)
	public EmailNotificationSender emailNotificationSender(NotificationSenderProperties properties,
			ObjectProvider<JavaMailSender> mailSenderProvider) {
		JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
		if (properties.email().enabled() && mailSender != null) {
			log.info("FR-003 이메일 발송: SMTP 실발송(SmtpEmailNotificationSender) 활성화.");
			return new SmtpEmailNotificationSender(mailSender, properties.email().from());
		}
		if (properties.email().enabled()) {
			// enabled=true 지만 JavaMailSender가 없음(spring.mail 설정 부재) → 실발송 불가, 로그 Mock으로 폴백.
			log.warn("FR-003 이메일 발송: notification.email.enabled=true 이지만 SMTP(JavaMailSender) 설정이 없어 "
					+ "로그 Mock으로 동작합니다. spring.mail.host 등을 설정하세요.");
		} else {
			log.info("FR-003 이메일 발송: 비활성(notification.email.enabled=false) → 로그 Mock으로 동작합니다.");
		}
		return new LoggingEmailNotificationSender();
	}

	@Bean
	@ConditionalOnMissingBean(PushNotificationSender.class)
	public PushNotificationSender pushNotificationSender(NotificationSenderProperties properties) {
		return new LoggingPushNotificationSender(properties.push().enabled());
	}
}
