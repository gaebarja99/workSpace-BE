package com.teamsync.back.auth;

import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.user.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization: Bearer <token> 헤더를 검증해 SecurityContext에 인증 정보를 채운다.
 *
 * 구성원 관리(P1) 회귀 방지: 토큰 클레임(발급 시점의 role/workspaceId 스냅샷)만으로 인증하면,
 * 발급 이후 관리자가 계정을 비활성화하거나(DEACTIVATED) 역할을 강등해도 만료 전까지 기존 토큰이
 * 그대로 통해버린다(강제 비활성화·강등이 최대 access-token 수명만큼 무력화됨). 그래서 서명 검증 후
 * PK 단건 조회로 계정을 다시 읽어, 상태(ACTIVE)와 역할(role)을 매 요청마다 최신 DB 값으로 갱신해
 * AuthenticatedUser/권한(authorities)을 구성한다 — workspaceId/email도 같은 이유로 DB 값을 쓴다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		String token = resolveToken(request);

		if (token != null && jwtTokenProvider.validateToken(token)) {
			Long userId = ((AuthenticatedUser) jwtTokenProvider.getAuthentication(token).getPrincipal()).userId();

			Optional<User> currentUser = userRepository.findById(userId)
					.filter(user -> user.getStatus() == UserStatus.ACTIVE);
			currentUser.ifPresent(user -> {
				AuthenticatedUser principal = new AuthenticatedUser(
						user.getId(), user.getWorkspace().getId(), user.getEmail(), user.getRole());
				List<GrantedAuthority> authorities =
						List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
				Authentication authentication = new UsernamePasswordAuthenticationToken(principal, token, authorities);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			});
			// 계정이 없거나(삭제) 비활성화 상태면 인증 정보를 채우지 않아 이후
			// authorizeHttpRequests에서 인증되지 않은 요청(401)으로 처리되게 한다.
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String header = request.getHeader(AUTHORIZATION_HEADER);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			return header.substring(BEARER_PREFIX.length());
		}
		return null;
	}
}
