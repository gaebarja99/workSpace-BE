package com.teamsync.back.auth;

import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * FR-002 JWT 발급/검증.
 * 매 요청마다 DB를 조회하지 않도록, 토큰 클레임(userId/workspaceId/role)만으로
 * AuthenticatedUser(경량 principal)를 복원한다.
 */
@Component
public class JwtTokenProvider {

	private final SecretKey key;
	private final JwtProperties jwtProperties;

	public JwtTokenProvider(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String generateAccessToken(User user) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + jwtProperties.accessTokenExpirationMs());

		return Jwts.builder()
				.subject(String.valueOf(user.getId()))
				.claim("email", user.getEmail())
				.claim("workspaceId", user.getWorkspace().getId())
				.claim("role", user.getRole().name())
				.issuedAt(now)
				.expiration(expiry)
				.signWith(key, Jwts.SIG.HS256)
				.compact();
	}

	public long getAccessTokenExpirationMs() {
		return jwtProperties.accessTokenExpirationMs();
	}

	public boolean validateToken(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	public Authentication getAuthentication(String token) {
		Claims claims = parseClaims(token);
		Long userId = Long.valueOf(claims.getSubject());
		Long workspaceId = claims.get("workspaceId", Long.class);
		String email = claims.get("email", String.class);
		Role role = Role.valueOf(claims.get("role", String.class));

		AuthenticatedUser principal = new AuthenticatedUser(userId, workspaceId, email, role);
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
		return new UsernamePasswordAuthenticationToken(principal, token, authorities);
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
