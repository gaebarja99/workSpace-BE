package com.teamsync.back.project.dto;

import com.teamsync.back.user.User;

/**
 * FR-301 담당자 선택용 프로젝트 멤버 목록(GET /api/projects/{projectId}/members) 응답.
 * 프로젝트 단위 멤버십 테이블이 없으므로 "프로젝트 멤버" = "해당 프로젝트가 속한 workspace의 모든 User"다.
 */
public record MemberSummaryResponse(
		Long userId,
		String name,
		String email
) {
	public static MemberSummaryResponse from(User user) {
		return new MemberSummaryResponse(user.getId(), user.getName(), user.getEmail());
	}
}
