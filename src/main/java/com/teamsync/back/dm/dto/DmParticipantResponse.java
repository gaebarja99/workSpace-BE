package com.teamsync.back.dm.dto;

import com.teamsync.back.user.User;

public record DmParticipantResponse(
		Long userId,
		String name
) {
	public static DmParticipantResponse from(User user) {
		return new DmParticipantResponse(user.getId(), user.getName());
	}
}
