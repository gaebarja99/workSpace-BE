package com.teamsync.back.config;

import com.teamsync.back.auth.JwtAuthenticationFilter;
import com.teamsync.back.auth.JwtTokenProvider;
import com.teamsync.back.auth.RestAccessDeniedHandler;
import com.teamsync.back.auth.RestAuthenticationEntryPoint;
import com.teamsync.back.user.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * FR-002 인증/권한 기초 설정.
 * - 세션 미사용(Stateless) + JWT 필터로 매 요청 인증
 * - /api/auth/** 만 인증 없이 허용, 그 외 전 API는 인증 필요
 * - 역할 기반 접근 제어는 @EnableMethodSecurity + @PreAuthorize 조합으로 구현(예: ProjectController)
 * - SSO(Google/MS)는 이번 단계 범위 밖이며, 이후 OAuth2 Client 설정을 이 필터 체인에 추가하는 방식으로 확장 가능
 * - CORS는 프론트(web, Next.js)가 브라우저에서 직접 호출하는 API(WebSocket, 클라이언트 컴포넌트
 *   fetch 등)를 위해 허용하며, 허용 오리진은 CORS_ALLOWED_ORIGINS 환경변수로 배포 환경마다 오버라이드한다.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Value("${teamsync.cors.allowed-origins}")
	private List<String> corsAllowedOrigins;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(corsAllowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider,
			RestAuthenticationEntryPoint authenticationEntryPoint, RestAccessDeniedHandler accessDeniedHandler,
			UserRepository userRepository)
			throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/**").permitAll()
						// 구성원 관리(P1): 초대 토큰 공개 조회(가입 전 사용자, 인증 불가 상태에서 열람)
						.requestMatchers("/api/invitations/**").permitAll()
						.requestMatchers("/actuator/health").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userRepository), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
