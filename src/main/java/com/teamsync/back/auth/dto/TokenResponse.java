package com.teamsync.back.auth.dto;

public record TokenResponse(
		String accessToken,
		String tokenType,
		long expiresInMs,
		UserSummary user
) {
	public static TokenResponse of(String accessToken, long expiresInMs, UserSummary user) {
		return new TokenResponse(accessToken, "Bearer", expiresInMs, user);
	}
}
