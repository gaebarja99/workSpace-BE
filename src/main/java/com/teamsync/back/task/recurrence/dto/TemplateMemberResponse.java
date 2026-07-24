package com.teamsync.back.task.recurrence.dto;

import com.teamsync.back.user.User;

/** FR-106 응답 내 담당자/생성자 요약(id+name만 노출). */
public record TemplateMemberResponse(
		Long id,
		String name
) {
	public static TemplateMemberResponse from(User user) {
		return new TemplateMemberResponse(user.getId(), user.getName());
	}
}
