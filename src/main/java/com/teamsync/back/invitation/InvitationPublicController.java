package com.teamsync.back.invitation;

import com.teamsync.back.invitation.dto.InvitationPreviewResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 구성원 관리(P1): 초대 토큰 공개 조회. 가입 전 사용자가 초대 링크를 열람할 때 사용하므로
 * 인증 불필요(SecurityConfig에 permitAll 등록).
 */
@RestController
@RequestMapping("/api/invitations")
public class InvitationPublicController {

	private final InvitationService invitationService;

	public InvitationPublicController(InvitationService invitationService) {
		this.invitationService = invitationService;
	}

	@GetMapping("/{token}")
	public ResponseEntity<InvitationPreviewResponse> preview(@PathVariable String token) {
		return ResponseEntity.ok(invitationService.previewByToken(token));
	}
}
