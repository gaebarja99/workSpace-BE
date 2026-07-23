package com.teamsync.back.channel.dto;

import com.teamsync.back.channel.message.Message;
import com.teamsync.back.channel.message.MessageType;
import com.teamsync.back.user.dto.MentionUser;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 채널 메시지 목록 조회 응답. 최상위 메시지/스레드 답글을 구분하지 않고 평평한 리스트로 내려주며,
 * parentMessageId가 null이면 최상위 메시지, 값이 있으면 해당 메시지에 대한 스레드 답글이다.
 * FR-301~305: messageType이 SYSTEM인 메시지는 author가 null이다(프론트는 messageType으로 뱃지만
 * 구분하며 author null을 별도 처리해야 한다).
 * FR-202-A/B: mentionedUsers(@멘션 대상)·mentionEveryone(@전체 여부)·reactions(이모지 반응 집계)를 함께 내려준다.
 * SYSTEM/TASK_COMMENT_SYNC 메시지처럼 멘션/반응이 없는 경우 각각 빈 배열/false로 안전하게 직렬화된다.
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
		LocalDateTime highlightedAt,
		List<MentionUser> mentionedUsers,
		boolean mentionEveryone,
		List<MessageReactionSummary> reactions
) {
	/**
	 * @param currentUserId 조회자 본인 id. reactions[].reactedByMe 계산에 쓰인다(항상 principal.userId() 전달).
	 */
	public static MessageResponse from(Message message, Long currentUserId) {
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
				message.getHighlightedAt(),
				message.getMentionedUsers().stream().map(MentionUser::from).toList(),
				message.isMentionEveryone(),
				MessageReactionSummary.summarize(message.getReactions(), currentUserId));
	}
}
