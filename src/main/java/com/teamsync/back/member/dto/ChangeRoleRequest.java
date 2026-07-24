package com.teamsync.back.member.dto;

import com.teamsync.back.user.Role;
import jakarta.validation.constraints.NotNull;

/** 구성원 관리(P1): PATCH /api/admin/members/{userId}/role 요청. */
public record ChangeRoleRequest(
		@NotNull(message = "역할은 필수입니다.")
		Role role
) {
}
