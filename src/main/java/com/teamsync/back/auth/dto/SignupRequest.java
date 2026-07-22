package com.teamsync.back.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * FR-000 회원가입 겸 워크스페이스 생성/합류 요청.
 * - 이메일 도메인과 일치하는 워크스페이스가 이미 있으면 workspaceName은 무시되고 해당 워크스페이스에 MEMBER로 합류한다.
 * - 일치하는 워크스페이스가 없으면 workspaceName으로 새 워크스페이스를 생성하고 ADMIN이 된다.
 */
public record SignupRequest(
		String workspaceName,

		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "이메일 형식이 올바르지 않습니다.")
		String email,

		@NotBlank(message = "비밀번호는 필수입니다.")
		@Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.")
		String password,

		@NotBlank(message = "이름은 필수입니다.")
		String name
) {
}
