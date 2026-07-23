package com.teamsync.back.channel.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * FR-202 메시지 생성 요청. parentMessageId가 지정되면 스레드 답글로 취급하며,
 * 서비스 계층에서 같은 channelId에 속하는 메시지인지 검증한다.
 */
public record MessageCreateRequest(
		@NotBlank(message = "메시지 내용은 필수입니다.")
		String content,

		Long parentMessageId
) {
}
