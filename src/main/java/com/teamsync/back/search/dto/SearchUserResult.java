package com.teamsync.back.search.dto;

import com.teamsync.back.user.User;

/**
 * FR-004(통합 검색) 사용자 검색 결과 항목. name/email 검색 매칭 대상.
 */
public record SearchUserResult(
		Long id,
		String name,
		String email
) {
	public static SearchUserResult from(User user) {
		return new SearchUserResult(user.getId(), user.getName(), user.getEmail());
	}
}
