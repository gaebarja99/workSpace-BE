package com.teamsync.back.channel.dto;

import com.teamsync.back.channel.message.Message;
import com.teamsync.back.channel.message.MessageType;
import java.time.LocalDateTime;

/**
 * 채널 메시지 목록 조회 응답. 최상위 메시지/스레드 답글을 구분하지 않고 평평한 리스트로 내려주며,
 * parentMessageId가 null이면 최상위 메시지, 값이 있으면 해당 메시지에 대한 스레드 답글이다.
 * FR-301~305: messageType이 SYSTEM인 메시지는 author가 null이다(프론트는 messageType으로 뱃지만
 * 구분하며 author null을 별도 처리해야 한다).
 */
public record MessageResponse(
		Long id,
		Long channelId,
		Long parentMessageId,
		String content,
		MessageAuthorResponse author,
		MessageType messageType,
		LocalDateTime createdAt,
		boolean pinned,
		LocalDateTime pinnedAt,
		boolean highlighted,
		LocalDateTime highlightedAt
) {
	public static MessageResponse from(Message message) {
		return new MessageResponse(
				message.getId(),
				message.getChannel().getId(),
				message.getParentMessage() != null ? message.getParentMessage().getId() : null,
				message.getContent(),
				message.getAuthor() != null ? MessageAuthorResponse.from(message.getAuthor()) : null,
				message.getMessageType(),
				message.getCreatedAt(),
				message.isPinned(),
				message.getPinnedAt(),
				message.isHighlighted(),
				message.getHighlightedAt());
	}
}
