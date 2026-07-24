package com.teamsync.back.invitation.dto;

import com.teamsync.back.invitation.Invitation;

/**
 * GET /api/invitations/{token} (공개 API) 응답.
 * valid=true면 email/workspaceName/role이 채워지고 reason은 null,
 * valid=false면 reason("NOT_FOUND"|"EXPIRED"|"REVOKED"|"ALREADY_ACCEPTED")만 유의미하다.
 */
public record InvitationPreviewResponse(
		String email,
		String workspaceName,
		String role,
		boolean valid,
		String reason
) {
	public static InvitationPreviewResponse valid(Invitation invitation) {
		return new InvitationPreviewResponse(
				invitation.getEmail(),
				invitation.getWorkspace().getName(),
				invitation.getRole().name(),
				true,
				null);
	}

	public static InvitationPreviewResponse invalid(String reason) {
		return new InvitationPreviewResponse(null, null, null, false, reason);
	}
}
