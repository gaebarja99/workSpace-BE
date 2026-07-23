package com.teamsync.back.channel.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * FR-202 메시지 생성 요청. parentMessageId가 지정되면 스레드 답글로 취급하며,
 * 서비스 계층에서 같은 channelId에 속하는 메시지인지 검증한다.
 * FR-202-A: mentionedUserIds는 optional(없으면 빈 배열). mentionEveryone은 기본 false이며, true면 해당 채널이
 * 속한 프로젝트(=워크스페이스) 멤버 전원(본인 제외)에게 @전체 멘션 알림을 생성한다.
 */
public record MessageCreateRequest(
		@NotBlank(message = "메시지 내용은 필수입니다.")
		String content,

		Long parentMessageId,

		List<Long> mentionedUserIds,

		boolean mentionEveryone
) {
}
