package com.teamsync.back.auth.sso;

import com.teamsync.back.auth.dto.TokenResponse;
import com.teamsync.back.auth.sso.dto.SsoAuthorizeResponse;
import com.teamsync.back.auth.sso.dto.SsoExchangeRequest;
import com.teamsync.back.auth.sso.dto.SsoProvidersResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-002 SSO(Google/Microsoft/Mock) 로그인.
 * 전 경로 /api/auth/** permitAll(SecurityConfig)이며, BE는 stateless JWT만 발급한다.
 * httpOnly 쿠키 설정은 FE Route Handler가 담당한다(기존 login/signup 패턴과 동일).
 */
@RestController
@RequestMapping("/api/auth/sso")
public class SsoAuthController {

	private final SsoService ssoService;

	public SsoAuthController(SsoService ssoService) {
		this.ssoService = ssoService;
	}

	/** 지원 공급자 목록과 활성화 여부. */
	@GetMapping("/providers")
	public ResponseEntity<SsoProvidersResponse> providers() {
		return ResponseEntity.ok(ssoService.listProviders());
	}

	/** authorization URL 발급. provider ∈ {google, microsoft, mock}, redirectUri = FE 콜백 절대 URL. */
	@GetMapping("/{provider}/authorize")
	public ResponseEntity<SsoAuthorizeResponse> authorize(
			@PathVariable String provider,
			@RequestParam String redirectUri) {
		return ResponseEntity.ok(ssoService.authorize(provider, redirectUri));
	}

	/** code 교환 + JIT 프로비저닝 -> 기존 TokenResponse 스키마로 JWT 발급. */
	@PostMapping("/{provider}/exchange")
	public ResponseEntity<TokenResponse> exchange(
			@PathVariable String provider,
			@Valid @RequestBody SsoExchangeRequest request) {
		return ResponseEntity.ok(ssoService.exchange(provider, request));
	}
}
