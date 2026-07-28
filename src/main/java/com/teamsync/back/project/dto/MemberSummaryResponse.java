package com.teamsync.back.project.dto;

import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;

/**
 * FR-301 담당자 선택용 프로젝트 멤버 목록(GET /api/projects/{projectId}/members) 및 후보/추가 응답.
 * project_members 테이블에 실제로 등록된 사용자만 "프로젝트 멤버"로 취급한다(V26).
 */
public record MemberSummaryResponse(
		Long userId,
		String name,
		String email,
		Role role
) {
	public static MemberSummaryResponse from(User user) {
		return new MemberSummaryResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
	}
}
