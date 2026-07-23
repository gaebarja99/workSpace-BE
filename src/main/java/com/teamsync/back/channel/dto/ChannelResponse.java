package com.teamsync.back.channel.dto;

import com.teamsync.back.channel.Channel;
import com.teamsync.back.channel.ChannelVisibility;
import java.time.LocalDateTime;

public record ChannelResponse(
		Long id,
		Long projectId,
		String name,
		ChannelVisibility visibility,
		LocalDateTime createdAt
) {
	public static ChannelResponse from(Channel channel) {
		return new ChannelResponse(
				channel.getId(),
				channel.getProject().getId(),
				channel.getName(),
				channel.getVisibility(),
				channel.getCreatedAt());
	}
}
