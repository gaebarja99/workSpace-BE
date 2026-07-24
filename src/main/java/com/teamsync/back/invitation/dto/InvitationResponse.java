package com.teamsync.back.invitation.dto;

import com.teamsync.back.invitation.Invitation;
import java.time.LocalDateTime;

public record InvitationResponse(
		Long id,
		String email,
		String role,
		String status,
		String invitedByName,
		LocalDateTime createdAt,
		LocalDateTime expiresAt
) {
	public static InvitationResponse from(Invitation invitation) {
		return new InvitationResponse(
				invitation.getId(),
				invitation.getEmail(),
				invitation.getRole().name(),
				invitation.getStatus().name(),
				invitation.getInvitedBy().getName(),
				invitation.getCreatedAt(),
				invitation.getExpiresAt());
	}
}
