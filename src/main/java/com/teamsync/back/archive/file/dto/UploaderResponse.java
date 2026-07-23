package com.teamsync.back.archive.file.dto;

import com.teamsync.back.user.User;

public record UploaderResponse(
		Long id,
		String name,
		String email
) {
	public static UploaderResponse from(User user) {
		return new UploaderResponse(user.getId(), user.getName(), user.getEmail());
	}
}
