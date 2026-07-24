package com.teamsync.back.member;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.LastActiveAdminException;
import com.teamsync.back.common.exception.LastAdminDemotionException;
import com.teamsync.back.common.exception.MemberNotFoundException;
import com.teamsync.back.common.exception.SelfDeactivationException;
import com.teamsync.back.invitation.InvitationRepository;
import com.teamsync.back.invitation.InvitationStatus;
import com.teamsync.back.member.dto.MemberResponse;
import com.teamsync.back.member.dto.MemberStatsResponse;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.user.UserStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구성원 관리(P1): 워크스페이스 구성원 목록/통계 조회 및 역할/활성상태 변경.
 * 리스크 대응(PRD 5.6): 클라이언트가 workspaceId를 지정하지 않고 항상 JWT(AuthenticatedUser)
 * 워크스페이스로만 대상 사용자를 스코핑(findByIdAndWorkspaceId)한다.
 */
@Service
public class MemberService {

	private final UserRepository userRepository;
	private final InvitationRepository invitationRepository;

	public MemberService(UserRepository userRepository, InvitationRepository invitationRepository) {
		this.userRepository = userRepository;
		this.invitationRepository = invitationRepository;
	}

	@Transactional(readOnly = true)
	public List<MemberResponse> listMembers(AuthenticatedUser principal) {
		return userRepository.findAllByWorkspaceIdOrderByCreatedAtAsc(principal.workspaceId()).stream()
				.map(MemberResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public MemberStatsResponse getStats(AuthenticatedUser principal) {
		Long workspaceId = principal.workspaceId();
		long totalMembers = userRepository.countByWorkspaceId(workspaceId);
		long activeMembers = userRepository.countByWorkspaceIdAndStatus(workspaceId, UserStatus.ACTIVE);
		long pendingInvitations = invitationRepository.countByWorkspaceIdAndStatus(workspaceId, InvitationStatus.PENDING);
		long adminCount = userRepository.countByWorkspaceIdAndRole(workspaceId, Role.ADMIN);
		return new MemberStatsResponse(totalMembers, activeMembers, pendingInvitations, adminCount);
	}

	@Transactional
	public MemberResponse changeRole(AuthenticatedUser principal, Long userId, Role newRole) {
		User target = userRepository.findByIdAndWorkspaceId(userId, principal.workspaceId())
				.orElseThrow(MemberNotFoundException::new);

		boolean isDemotionFromAdmin = target.getRole() == Role.ADMIN && newRole != Role.ADMIN;
		if (isDemotionFromAdmin) {
			long adminCount = userRepository.countByWorkspaceIdAndRole(principal.workspaceId(), Role.ADMIN);
			if (adminCount <= 1) {
				throw new LastAdminDemotionException();
			}
		}

		target.changeRole(newRole);
		return MemberResponse.from(target);
	}

	@Transactional
	public MemberResponse changeStatus(AuthenticatedUser principal, Long userId, UserStatus newStatus) {
		User target = userRepository.findByIdAndWorkspaceId(userId, principal.workspaceId())
				.orElseThrow(MemberNotFoundException::new);

		if (newStatus == UserStatus.DEACTIVATED) {
			if (target.getId().equals(principal.userId())) {
				throw new SelfDeactivationException();
			}
			if (target.getRole() == Role.ADMIN) {
				long activeAdminCount = userRepository
						.countByWorkspaceIdAndRoleAndStatus(principal.workspaceId(), Role.ADMIN, UserStatus.ACTIVE);
				if (activeAdminCount <= 1) {
					throw new LastActiveAdminException();
				}
			}
		}

		target.changeStatus(newStatus);
		return MemberResponse.from(target);
	}
}
