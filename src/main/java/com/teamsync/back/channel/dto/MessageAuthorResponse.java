package com.teamsync.back.channel.dto;

import com.teamsync.back.user.User;

public record MessageAuthorResponse(
		Long id,
		String name,
		String email
) {
	public static MessageAuthorResponse from(User user) {
		return new MessageAuthorResponse(user.getId(), user.getName(), user.getEmail());
	}
}
