package com.teamsync.back.channel.dto;

import com.teamsync.back.channel.message.Message;
import java.time.LocalDateTime;

/**
 * 채널 메시지 목록 조회 응답. 최상위 메시지/스레드 답글을 구분하지 않고 평평한 리스트로 내려주며,
 * parentMessageId가 null이면 최상위 메시지, 값이 있으면 해당 메시지에 대한 스레드 답글이다.
 */
public record MessageResponse(
		Long id,
		Long channelId,
		Long parentMessageId,
		String content,
		MessageAuthorResponse author,
		LocalDateTime createdAt
) {
	public static MessageResponse from(Message message) {
		return new MessageResponse(
				message.getId(),
				message.getChannel().getId(),
				message.getParentMessage() != null ? message.getParentMessage().getId() : null,
				message.getContent(),
				MessageAuthorResponse.from(message.getAuthor()),
				message.getCreatedAt());
	}
}
