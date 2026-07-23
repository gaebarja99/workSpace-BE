package com.teamsync.back.dm.dto;

import com.teamsync.back.user.User;

/** GET /api/dm/contacts: 새 DM을 시작할 때 고를 수 있는 워크스페이스 내 사용자(호출자 본인 제외). */
public record DmContactResponse(
		Long userId,
		String name,
		String email
) {
	public static DmContactResponse from(User user) {
		return new DmContactResponse(user.getId(), user.getName(), user.getEmail());
	}
}
