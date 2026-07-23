package com.teamsync.back.dm.dto;

import com.teamsync.back.dm.DmMessage;
import java.time.LocalDateTime;

public record DmLastMessageResponse(
		String content,
		String authorName,
		LocalDateTime createdAt
) {
	public static DmLastMessageResponse from(DmMessage message) {
		return new DmLastMessageResponse(message.getContent(), message.getAuthor().getName(), message.getCreatedAt());
	}
}
