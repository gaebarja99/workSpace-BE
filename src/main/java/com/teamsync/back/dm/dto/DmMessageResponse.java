package com.teamsync.back.dm.dto;

import com.teamsync.back.dm.DmMessage;
import java.time.LocalDateTime;

public record DmMessageResponse(
		Long id,
		Long authorId,
		String authorName,
		String content,
		LocalDateTime createdAt
) {
	public static DmMessageResponse from(DmMessage message) {
		return new DmMessageResponse(
				message.getId(),
				message.getAuthor().getId(),
				message.getAuthor().getName(),
				message.getContent(),
				message.getCreatedAt());
	}
}
