package com.teamsync.back.member.dto;

import com.teamsync.back.user.User;
import java.time.LocalDateTime;

/** 구성원 관리(P1): GET /api/admin/members, PATCH .../role, PATCH .../status 공통 응답 shape. */
public record MemberResponse(
		Long id,
		String name,
		String email,
		String role,
		String status,
		String authProvider,
		LocalDateTime joinedAt
) {
	public static MemberResponse from(User user) {
		return new MemberResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole().name(),
				user.getStatus().name(),
				user.getAuthProvider().name(),
				user.getCreatedAt());
	}
}
