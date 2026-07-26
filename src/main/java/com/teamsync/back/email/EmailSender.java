package com.teamsync.back.email;

/**
 * 이메일 발송기. 기본 구현은 로그 기반 Mock({@link LoggingEmailSender})이며,
 * email.enabled=true & SMTP(JavaMailSender) 설정이 있으면 실발송({@link SmtpEmailSender})으로 대체된다.
 * 팀 채팅/알림 기능 제거 이후에도 구성원 초대(InvitationService)와 보고서 내보내기(ReportExportService,
 * FR-409)가 실제로 이메일을 발송해야 하므로, 원래 notification 패키지 전용이 아닌 공용 인프라로 이 패키지에 둔다.
 */
public interface EmailSender {

	/**
	 * 구성원 관리(P1): 초대 대상은 아직 User가 아니므로(가입 전) 이메일 문자열 기반으로 발송한다.
	 */
	void send(String toEmail, String subject, String body);

	/**
	 * FR-409(보고서 내보내기): HTML 본문(Slack-style 카드 요약)과 선택적 첨부파일(PDF)을 함께 보낸다.
	 * attachmentBytes가 null이거나 길이 0이면 첨부 없이 본문만 발송한다.
	 */
	void sendHtmlWithAttachment(String toEmail, String subject, String htmlBody, String attachmentFilename,
			byte[] attachmentBytes, String attachmentContentType);

	/**
	 * FR-409 응답의 mocked 플래그 판정용: 이 발송기가 실제 SMTP로 나가지 않고 로그 Mock으로 동작
	 * 중이면 true. EmailSenderConfig가 두 구현 중 하나를 빈으로 등록하므로 별도 설정 조회 없이
	 * 구현체 스스로 답한다.
	 */
	boolean isMock();
}
