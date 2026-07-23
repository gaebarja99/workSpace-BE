package com.teamsync.back.archive.dto;

import com.teamsync.back.user.User;

public record ArchiveAuthorResponse(
		Long id,
		String name,
		String email
) {
	public static ArchiveAuthorResponse from(User user) {
		return new ArchiveAuthorResponse(user.getId(), user.getName(), user.getEmail());
	}
}
