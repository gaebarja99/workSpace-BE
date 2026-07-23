package com.teamsync.back.auth.sso;

import com.teamsync.back.auth.JwtTokenProvider;
import com.teamsync.back.auth.dto.TokenResponse;
import com.teamsync.back.auth.dto.UserSummary;
import com.teamsync.back.auth.sso.dto.SsoAuthorizeResponse;
import com.teamsync.back.auth.sso.dto.SsoExchangeRequest;
import com.teamsync.back.auth.sso.dto.SsoProviderInfo;
import com.teamsync.back.auth.sso.dto.SsoProvidersResponse;
import com.teamsync.back.common.exception.SsoNoWorkspaceException;
import com.teamsync.back.common.exception.SsoProviderDisabledException;
import com.teamsync.back.user.AuthProvider;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.workspace.Workspace;
import com.teamsync.back.workspace.WorkspaceRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-002 SSO: 공급자 목록/authorize/exchange 오케스트레이션 + JIT 프로비저닝.
 * 워크스페이스는 기존 이메일 가입과 동일하게 이메일 도메인으로 매칭하며(AuthService.signup 계승),
 * 성공 시 기존 TokenResponse 스키마 그대로 JWT를 발급한다.
 */
@Service
public class SsoService {

	private final Map<String, SsoProvider> providers;
	private final SsoStateService ssoStateService;
	private final WorkspaceRepository workspaceRepository;
	private final UserRepository userRepository;
	private final JwtTokenProvider jwtTokenProvider;

	public SsoService(List<SsoProvider> providers, SsoStateService ssoStateService,
			WorkspaceRepository workspaceRepository, UserRepository userRepository,
			JwtTokenProvider jwtTokenProvider) {
		// 계약상 노출 순서(google, microsoft, mock)를 유지하기 위해 순서 있는 맵을 사용한다.
		Map<String, SsoProvider> ordered = new LinkedHashMap<>();
		for (SsoProvider provider : providers) {
			ordered.put(provider.name(), provider);
		}
		this.providers = ordered;
		this.ssoStateService = ssoStateService;
		this.workspaceRepository = workspaceRepository;
		this.userRepository = userRepository;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	/** 지원 공급자와 활성화 여부. FE가 enabled인 버튼만 활성화한다. */
	public SsoProvidersResponse listProviders() {
		List<SsoProviderInfo> list = providers.values().stream()
				.map(p -> new SsoProviderInfo(p.name(), p.isEnabled()))
				.toList();
		return new SsoProvidersResponse(list);
	}

	/** authorization URL 발급. 미지원/미설정 provider면 SSO_PROVIDER_DISABLED. */
	public SsoAuthorizeResponse authorize(String providerName, String redirectUri) {
		SsoProvider provider = resolveEnabled(providerName);
		String state = ssoStateService.sign(providerName, redirectUri);
		return new SsoAuthorizeResponse(provider.buildAuthorizationUrl(redirectUri, state));
	}

	/** code 교환 + JIT 프로비저닝 후 JWT 발급. */
	@Transactional
	public TokenResponse exchange(String providerName, SsoExchangeRequest request) {
		SsoProvider provider = resolveEnabled(providerName);
		// state 서명/만료/provider/redirectUri 일치 검증(불일치 -> SSO_INVALID_STATE).
		ssoStateService.verify(request.state(), providerName, request.redirectUri());

		SsoUserInfo info = provider.exchange(request.code(), request.redirectUri());

		String email = info.email().trim().toLowerCase();
		String domain = extractDomain(email);
		Workspace workspace = workspaceRepository.findByDomain(domain)
				.orElseThrow(() -> new SsoNoWorkspaceException(domain));

		AuthProvider authProvider = toAuthProvider(providerName);
		User user = userRepository.findByEmail(email)
				.orElseGet(() -> userRepository.save(
						new User(workspace, email, safeName(info.name(), email), Role.MEMBER, authProvider)));

		return issueToken(user);
	}

	private SsoProvider resolveEnabled(String providerName) {
		SsoProvider provider = providers.get(providerName);
		if (provider == null || !provider.isEnabled()) {
			throw new SsoProviderDisabledException(providerName);
		}
		return provider;
	}

	private AuthProvider toAuthProvider(String providerName) {
		return switch (providerName) {
			case "google" -> AuthProvider.GOOGLE;
			case "microsoft" -> AuthProvider.MICROSOFT;
			case "mock" -> AuthProvider.MOCK;
			default -> throw new SsoProviderDisabledException(providerName);
		};
	}

	private TokenResponse issueToken(User user) {
		String accessToken = jwtTokenProvider.generateAccessToken(user);
		return TokenResponse.of(accessToken, jwtTokenProvider.getAccessTokenExpirationMs(), UserSummary.from(user));
	}

	private String safeName(String name, String email) {
		if (name != null && !name.isBlank()) {
			return name.trim();
		}
		return email.substring(0, email.indexOf('@'));
	}

	private String extractDomain(String email) {
		int at = email.indexOf('@');
		return email.substring(at + 1);
	}
}
