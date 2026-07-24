package com.teamsync.back.invitation;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.invitation.dto.InvitationCreateRequest;
import com.teamsync.back.invitation.dto.InvitationResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 구성원 관리(P1): 관리자의 초대 발급/조회/철회. */
@RestController
@RequestMapping("/api/admin/members/invitations")
public class InvitationAdminController {

	private final InvitationService invitationService;

	public InvitationAdminController(InvitationService invitationService) {
		this.invitationService = invitationService;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<InvitationResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody InvitationCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(invitationService.createInvitation(principal, request));
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<InvitationResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(invitationService.listPendingInvitations(principal));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> revoke(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
		invitationService.revokeInvitation(principal, id);
		return ResponseEntity.noContent().build();
	}
}
