package com.teamsync.back.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "teamsync.jwt")
public record JwtProperties(
		String secret,
		long accessTokenExpirationMs
) {
}
