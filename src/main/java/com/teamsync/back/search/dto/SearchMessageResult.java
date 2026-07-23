package com.teamsync.back.search.dto;

import com.teamsync.back.channel.message.Message;
import java.time.LocalDateTime;

/**
 * FR-004(통합 검색) 메시지 검색 결과 항목. SYSTEM 타입 메시지는 검색 대상에서 이미 제외되어 넘어오므로
 * (MessageRepository#searchByWorkspace 참고) authorName은 이론상 항상 값이 있지만, 프론트에서는
 * 방어적으로 nullable 취급해야 한다(FR-301~305 작업 당시 author null 처리 누락 사고 이력 참고).
 */
public record SearchMessageResult(
		Long id,
		Long channelId,
		String channelName,
		Long projectId,
		String projectName,
		String authorName,
		String content,
		LocalDateTime createdAt,
		Long parentMessageId,
		boolean pinned
) {
	public static SearchMessageResult from(Message message) {
		return new SearchMessageResult(
				message.getId(),
				message.getChannel().getId(),
				message.getChannel().getName(),
				message.getChannel().getProject().getId(),
				message.getChannel().getProject().getName(),
				message.getAuthor() != null ? message.getAuthor().getName() : null,
				message.getContent(),
				message.getCreatedAt(),
				message.getParentMessage() != null ? message.getParentMessage().getId() : null,
				message.isPinned());
	}
}
