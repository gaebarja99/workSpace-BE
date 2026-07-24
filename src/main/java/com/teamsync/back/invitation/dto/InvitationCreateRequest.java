package com.teamsync.back.invitation.dto;

import com.teamsync.back.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 구성원 관리(P1): 관리자 초대 생성 요청. role은 LEADER/MEMBER/GUEST만 허용(ADMIN은 서비스에서 거부). */
public record InvitationCreateRequest(
		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "이메일 형식이 올바르지 않습니다.")
		String email,

		@NotNull(message = "역할은 필수입니다.")
		Role role
) {
}
