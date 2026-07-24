package com.teamsync.back.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /api/auth/signup/invitation (공개 API) 요청. */
public record InvitationSignupRequest(
		@NotBlank(message = "토큰은 필수입니다.")
		String token,

		@NotBlank(message = "이름은 필수입니다.")
		String name,

		@NotBlank(message = "비밀번호는 필수입니다.")
		@Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.")
		String password
) {
}
