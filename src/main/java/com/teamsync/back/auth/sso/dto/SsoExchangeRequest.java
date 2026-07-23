package com.teamsync.back.auth.sso.dto;

import jakarta.validation.constraints.NotBlank;

/** FR-002 SSO: POST /api/auth/sso/{provider}/exchange 요청. */
public record SsoExchangeRequest(
		@NotBlank(message = "code는 필수입니다.")
		String code,

		@NotBlank(message = "redirectUri는 필수입니다.")
		String redirectUri,

		@NotBlank(message = "state는 필수입니다.")
		String state
) {
}
