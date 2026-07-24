package com.teamsync.back.invitation;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.auth.JwtTokenProvider;
import com.teamsync.back.auth.dto.TokenResponse;
import com.teamsync.back.auth.dto.UserSummary;
import com.teamsync.back.common.exception.DuplicateInvitationException;
import com.teamsync.back.common.exception.DuplicateMemberException;
import com.teamsync.back.common.exception.InvalidInvitationException;
import com.teamsync.back.common.exception.InvalidInvitationRoleException;
import com.teamsync.back.common.exception.InvitationNotFoundException;
import com.teamsync.back.invitation.dto.InvitationCreateRequest;
import com.teamsync.back.invitation.dto.InvitationPreviewResponse;
import com.teamsync.back.invitation.dto.InvitationResponse;
import com.teamsync.back.invitation.dto.InvitationSignupRequest;
import com.teamsync.back.notification.sender.EmailNotificationSender;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.workspace.Workspace;
import com.teamsync.back.workspace.WorkspaceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구성원 관리/초대·승인(P1): 관리자 초대 발급/조회/철회, 초대 토큰 공개 조회, 토큰 기반 가입.
 * 기존 도메인 자동가입(AuthService.signup/SsoService.exchange)과는 별개의 경로이며,
 * 초대 대상은 가입 전까지 User가 아니다(EmailNotificationSender의 이메일 문자열 오버로드로 발송).
 */
@Service
public class InvitationService {

	private static final int EXPIRATION_DAYS = 7;

	private final InvitationRepository invitationRepository;
	private final UserRepository userRepository;
	private final WorkspaceRepository workspaceRepository;
	private final EmailNotificationSender emailNotificationSender;
	private final InvitationProperties invitationProperties;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	public InvitationService(InvitationRepository invitationRepository, UserRepository userRepository,
			WorkspaceRepository workspaceRepository, EmailNotificationSender emailNotificationSender,
			InvitationProperties invitationProperties, PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider) {
		this.invitationRepository = invitationRepository;
		this.userRepository = userRepository;
		this.workspaceRepository = workspaceRepository;
		this.emailNotificationSender = emailNotificationSender;
		this.invitationProperties = invitationProperties;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Transactional
	public InvitationResponse createInvitation(AuthenticatedUser principal, InvitationCreateRequest request) {
		String email = request.email().trim().toLowerCase();

		if (request.role() == Role.ADMIN) {
			throw new InvalidInvitationRoleException();
		}
		if (userRepository.existsByEmail(email)) {
			throw new DuplicateMemberException(email);
		}

		LocalDateTime now = LocalDateTime.now();
		Optional<Invitation> existingPending = invitationRepository
				.findByWorkspaceIdAndEmailAndStatus(principal.workspaceId(), email, InvitationStatus.PENDING);
		if (existingPending.isPresent()) {
			Invitation existing = existingPending.get();
			if (!existing.isExpired(now)) {
				throw new DuplicateInvitationException(email);
			}
			// 만료된 PENDING은 새 초대 생성을 막지 않도록 lazy 정리한다.
			existing.markExpired();
		}

		Workspace workspace = workspaceRepository.getReferenceById(principal.workspaceId());
		User invitedBy = userRepository.getReferenceById(principal.userId());

		String token = UUID.randomUUID().toString();
		Invitation invitation = invitationRepository.save(
				new Invitation(workspace, email, request.role(), token, invitedBy, now.plusDays(EXPIRATION_DAYS)));

		sendInvitationEmail(invitation);

		return InvitationResponse.from(invitation);
	}

	@Transactional(readOnly = true)
	public List<InvitationResponse> listPendingInvitations(AuthenticatedUser principal) {
		return invitationRepository
				.findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(principal.workspaceId(), InvitationStatus.PENDING)
				.stream()
				.map(InvitationResponse::from)
				.toList();
	}

	@Transactional
	public void revokeInvitation(AuthenticatedUser principal, Long id) {
		Invitation invitation = invitationRepository.findByIdAndWorkspaceId(id, principal.workspaceId())
				.orElseThrow(InvitationNotFoundException::new);
		invitation.markRevoked();
	}

	@Transactional
	public InvitationPreviewResponse previewByToken(String token) {
		Optional<Invitation> found = invitationRepository.findByToken(token);
		if (found.isEmpty()) {
			return InvitationPreviewResponse.invalid("NOT_FOUND");
		}

		Invitation invitation = found.get();
		markExpiredIfNeeded(invitation);

		return switch (invitation.getStatus()) {
			case PENDING -> InvitationPreviewResponse.valid(invitation);
			case ACCEPTED -> InvitationPreviewResponse.invalid("ALREADY_ACCEPTED");
			case REVOKED -> InvitationPreviewResponse.invalid("REVOKED");
			case EXPIRED -> InvitationPreviewResponse.invalid("EXPIRED");
		};
	}

	@Transactional
	public TokenResponse signupViaInvitation(InvitationSignupRequest request) {
		Invitation invitation = invitationRepository.findByToken(request.token())
				.orElseThrow(InvalidInvitationException::new);
		markExpiredIfNeeded(invitation);

		if (invitation.getStatus() != InvitationStatus.PENDING) {
			throw new InvalidInvitationException();
		}

		User user = userRepository.save(new User(invitation.getWorkspace(), invitation.getEmail(),
				passwordEncoder.encode(request.password()), request.name().trim(), invitation.getRole()));
		invitation.markAccepted();

		String accessToken = jwtTokenProvider.generateAccessToken(user);
		return TokenResponse.of(accessToken, jwtTokenProvider.getAccessTokenExpirationMs(), UserSummary.from(user));
	}

	private void markExpiredIfNeeded(Invitation invitation) {
		if (invitation.getStatus() == InvitationStatus.PENDING && invitation.isExpired(LocalDateTime.now())) {
			invitation.markExpired();
		}
	}

	private void sendInvitationEmail(Invitation invitation) {
		String link = invitationProperties.frontendBaseUrl() + "/invite/" + invitation.getToken();
		String subject = "TeamSync 워크스페이스 초대";
		String body = "TeamSync 워크스페이스에 초대되었습니다. 아래 링크에서 가입을 완료해주세요.\n" + link;
		emailNotificationSender.send(invitation.getEmail(), subject, body);
	}
}
