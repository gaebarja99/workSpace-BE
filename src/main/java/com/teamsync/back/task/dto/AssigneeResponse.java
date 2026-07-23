package com.teamsync.back.task.dto;

import com.teamsync.back.user.User;

public record AssigneeResponse(
		Long id,
		String name,
		String email
) {
	public static AssigneeResponse from(User user) {
		return new AssigneeResponse(user.getId(), user.getName(), user.getEmail());
	}
}
