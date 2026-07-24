package com.teamsync.back.notification.sender;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * FR-003 발송기 빈 구성 검증(DB/웹 컨텍스트 불필요).
 * QA 결함 #1 회귀 방지: notification.email.enabled=true 이고 JavaMailSender가 있으면 실제로
 * SmtpEmailNotificationSender가 주입되어야 한다(과거 @ConditionalOnBean 안티패턴에서는 항상 Mock이 승리했음).
 */
class NotificationSenderConfigTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(NotificationSenderConfig.class);

	@Test
	void 이메일_비활성_기본이면_로그_Mock_발송기가_주입된다() {
		runner.withBean(NotificationSenderProperties.class, () -> props(false))
				.run(context -> assertThat(context.getBean(EmailNotificationSender.class))
						.isInstanceOf(LoggingEmailNotificationSender.class));
	}

	@Test
	void 이메일_활성_그리고_JavaMailSender가_있으면_SMTP_실발송기가_주입된다() {
		runner.withBean(NotificationSenderProperties.class, () -> props(true))
				.withBean(JavaMailSender.class, () -> Mockito.mock(JavaMailSender.class))
				.run(context -> assertThat(context.getBean(EmailNotificationSender.class))
						.isInstanceOf(SmtpEmailNotificationSender.class));
	}

	@Test
	void 이메일_활성이어도_JavaMailSender가_없으면_로그_Mock으로_폴백한다() {
		runner.withBean(NotificationSenderProperties.class, () -> props(true))
				.run(context -> assertThat(context.getBean(EmailNotificationSender.class))
						.isInstanceOf(LoggingEmailNotificationSender.class));
	}

	@Test
	void 푸시는_항상_로그_Mock_발송기가_주입된다() {
		runner.withBean(NotificationSenderProperties.class, () -> props(false))
				.run(context -> assertThat(context.getBean(PushNotificationSender.class))
						.isInstanceOf(LoggingPushNotificationSender.class));
	}

	private static NotificationSenderProperties props(boolean emailEnabled) {
		return new NotificationSenderProperties(
				new NotificationSenderProperties.Email(emailEnabled, "no-reply@teamsync.local"),
				new NotificationSenderProperties.Push(false));
	}
}
