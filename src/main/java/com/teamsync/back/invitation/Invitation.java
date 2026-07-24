package com.teamsync.back.invitation;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.workspace.Workspace;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 구성원 관리(P1): 워크스페이스 관리자가 발급하는 초대. 아직 User가 아닌 이메일 대상으로,
 * 토큰 기반 가입(POST /api/auth/signup/invitation) 경로를 통해서만 User로 전환된다.
 * 기존 도메인 자동가입(AuthService.signup, SsoService.exchange - 이메일 도메인 매칭)과는
 * 별개의 흐름이다.
 */
@Getter
@Entity
@Table(name = "invitations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@Column(nullable = false, length = 255)
	private String email;

	// ADMIN은 초대 대상 역할로 허용하지 않는다(InvitationService에서 재검증, DB CHECK도 병행).
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InvitationStatus status;

	@Column(nullable = false, unique = true, length = 255)
	private String token;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "invited_by_user_id", nullable = false)
	private User invitedBy;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	public Invitation(Workspace workspace, String email, Role role, String token, User invitedBy,
			LocalDateTime expiresAt) {
		this.workspace = workspace;
		this.email = email;
		this.role = role;
		this.status = InvitationStatus.PENDING;
		this.token = token;
		this.invitedBy = invitedBy;
		this.expiresAt = expiresAt;
	}

	public boolean isExpired(LocalDateTime now) {
		return expiresAt.isBefore(now);
	}

	public void markAccepted() {
		this.status = InvitationStatus.ACCEPTED;
	}

	public void markRevoked() {
		this.status = InvitationStatus.REVOKED;
	}

	public void markExpired() {
		this.status = InvitationStatus.EXPIRED;
	}
}
