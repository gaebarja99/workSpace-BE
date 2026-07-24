package com.teamsync.back.member.dto;

import com.teamsync.back.user.UserStatus;
import jakarta.validation.constraints.NotNull;

/** 구성원 관리(P1): PATCH /api/admin/members/{userId}/status 요청. */
public record ChangeStatusRequest(
		@NotNull(message = "상태는 필수입니다.")
		UserStatus status
) {
}
