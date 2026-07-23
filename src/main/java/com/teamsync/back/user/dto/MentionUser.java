package com.teamsync.back.user.dto;

import com.teamsync.back.user.User;

/**
 * FR-105-A(태스크 댓글 @멘션) / FR-202-A(메시지 @멘션) 공통: 멘션된 사용자를 응답에 노출할 때 쓰는
 * 최소 표현(id + 표시 이름). 이메일 등 민감 정보는 담지 않는다.
 */
public record MentionUser(
		Long id,
		String name
) {
	public static MentionUser from(User user) {
		return new MentionUser(user.getId(), user.getName());
	}
}
