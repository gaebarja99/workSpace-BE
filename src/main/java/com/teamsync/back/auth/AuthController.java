package com.teamsync.back.auth;

import com.teamsync.back.auth.dto.LoginRequest;
import com.teamsync.back.auth.dto.SignupRequest;
import com.teamsync.back.auth.dto.TokenResponse;
import com.teamsync.back.auth.dto.UserSummary;
import com.teamsync.back.invitation.InvitationService;
import com.teamsync.back.invitation.dto.InvitationSignupRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FR-000, FR-002: 회원가입(워크스페이스 생성/합류 겸용), 로그인, 내 정보 조회. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final InvitationService invitationService;

	public AuthController(AuthService authService, InvitationService invitationService) {
		this.authService = authService;
		this.invitationService = invitationService;
	}

	@PostMapping("/signup")
	public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
	}

	// 구성원 관리(P1): 초대 토큰 기반 가입. 도메인 자동가입(signup)과 별개 경로이며, 응답 포맷은 동일하다.
	@PostMapping("/signup/invitation")
	public ResponseEntity<TokenResponse> signupViaInvitation(@Valid @RequestBody InvitationSignupRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(invitationService.signupViaInvitation(request));
	}

	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<UserSummary> me(@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(authService.getCurrentUser(principal));
	}
}
