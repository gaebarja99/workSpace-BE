package com.teamsync.back.auth.dto;

import com.teamsync.back.user.User;

public record UserSummary(
		Long id,
		String email,
		String name,
		String role,
		Long workspaceId,
		String workspaceName
) {
	public static UserSummary from(User user) {
		return new UserSummary(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getRole().name(),
				user.getWorkspace().getId(),
				user.getWorkspace().getName());
	}
}
