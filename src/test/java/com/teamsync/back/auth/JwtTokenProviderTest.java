package com.teamsync.back.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.workspace.Workspace;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Spring 컨텍스트/DB 없이 순수 JWT 발급·검증 로직만 검증하는 단위 테스트.
 * Workspace/User는 Repository 없이 리플렉션으로 id를 주입해 도메인 순수성을 유지한다.
 */
class JwtTokenProviderTest {

	private final JwtProperties jwtProperties = new JwtProperties(
			"test-only-secret-key-must-be-at-least-32-bytes-long!!", 60_000L);
	private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties);

	@Test
	void generateAccessToken_그리고_검증하면_클레임이_복원된다() throws Exception {
		Workspace workspace = new Workspace("그로우테크", "growtech.io");
		setId(workspace, 10L);
		User user = new User(workspace, "park.junior@growtech.io", "hashed", "박사원", Role.MEMBER);
		setId(user, 1L);

		String token = jwtTokenProvider.generateAccessToken(user);

		assertThat(jwtTokenProvider.validateToken(token)).isTrue();

		Authentication authentication = jwtTokenProvider.getAuthentication(token);
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();

		assertThat(principal.userId()).isEqualTo(1L);
		assertThat(principal.workspaceId()).isEqualTo(10L);
		assertThat(principal.email()).isEqualTo("park.junior@growtech.io");
		assertThat(principal.role()).isEqualTo(Role.MEMBER);
		assertThat(authentication.getAuthorities())
				.extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_MEMBER");
	}

	@Test
	void 위조된_토큰은_검증에_실패한다() {
		assertThat(jwtTokenProvider.validateToken("not-a-valid-jwt")).isFalse();
	}

	private void setId(Object entity, Long id) throws Exception {
		Field idField = entity.getClass().getDeclaredField("id");
		idField.setAccessible(true);
		idField.set(entity, id);
	}
}
