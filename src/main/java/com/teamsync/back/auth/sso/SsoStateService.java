package com.teamsync.back.auth.sso;

import com.teamsync.back.auth.JwtProperties;
import com.teamsync.back.common.exception.SsoInvalidStateException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * FR-002 SSO: stateless CSRF 방지용 state 토큰 서명/검증.
 * 세션 저장 없이 짧은 서명 JWT로 provider/redirectUri/발급시각을 담아 만료(5분)까지 검증한다.
 * 서명 키는 기존 인증 키(teamsync.jwt.secret)를 재사용한다(과설계 방지). 용도 구분을 위해
 * purpose 클레임을 고정 값으로 넣어 액세스 토큰과 혼용되지 않도록 한다.
 */
@Component
public class SsoStateService {

	private static final String PURPOSE = "sso_state";
	private static final long STATE_EXPIRATION_MS = 5 * 60 * 1000L; // 5분

	private final SecretKey key;

	public SsoStateService(JwtProperties jwtProperties) {
		this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	/** provider/redirectUri를 담은 서명 state 토큰을 발급한다(만료 5분). */
	public String sign(String provider, String redirectUri) {
		Date now = new Date();
		return Jwts.builder()
				.claim("purpose", PURPOSE)
				.claim("provider", provider)
				.claim("redirectUri", redirectUri)
				.issuedAt(now)
				.expiration(new Date(now.getTime() + STATE_EXPIRATION_MS))
				.signWith(key, Jwts.SIG.HS256)
				.compact();
	}

	/**
	 * state의 서명/만료를 검증하고, exchange 요청의 provider/redirectUri와 일치하는지 확인한다.
	 * 하나라도 불일치하면 {@link SsoInvalidStateException}(400 SSO_INVALID_STATE).
	 */
	public void verify(String state, String provider, String redirectUri) {
		if (state == null || state.isBlank()) {
			throw new SsoInvalidStateException();
		}
		final Claims claims;
		try {
			claims = Jwts.parser()
					.verifyWith(key)
					.build()
					.parseSignedClaims(state)
					.getPayload();
		} catch (JwtException | IllegalArgumentException ex) {
			throw new SsoInvalidStateException();
		}

		if (!PURPOSE.equals(claims.get("purpose", String.class))
				|| !Objects.equals(provider, claims.get("provider", String.class))
				|| !Objects.equals(redirectUri, claims.get("redirectUri", String.class))) {
			throw new SsoInvalidStateException();
		}
	}
}
