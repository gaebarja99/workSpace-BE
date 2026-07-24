package com.teamsync.back.invitation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.auth.JwtTokenProvider;
import com.teamsync.back.common.exception.DuplicateInvitationException;
import com.teamsync.back.common.exception.DuplicateMemberException;
import com.teamsync.back.common.exception.InvalidInvitationRoleException;
import com.teamsync.back.invitation.dto.InvitationCreateRequest;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.workspace.WorkspaceRepository;
import com.teamsync.back.notification.sender.EmailNotificationSender;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 구성원 관리(P1) 초대 생성의 핵심 가드레일 단위 테스트: 중복 구성원/중복 초대/ADMIN 초대 금지.
 */
@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

	@Mock
	private InvitationRepository invitationRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private WorkspaceRepository workspaceRepository;

	@Mock
	private EmailNotificationSender emailNotificationSender;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	private InvitationService invitationService;
	private AuthenticatedUser adminPrincipal;

	@BeforeEach
	void setUp() {
		InvitationProperties properties = new InvitationProperties("http://localhost:3000");
		invitationService = new InvitationService(invitationRepository, userRepository, workspaceRepository,
				emailNotificationSender, properties, passwordEncoder, jwtTokenProvider);
		adminPrincipal = new AuthenticatedUser(1L, 10L, "admin@growtech.io", Role.ADMIN);
	}

	@Test
	void ADMIN_역할로_초대하면_예외() {
		var request = new InvitationCreateRequest("newbie@growtech.io", Role.ADMIN);

		assertThatThrownBy(() -> invitationService.createInvitation(adminPrincipal, request))
				.isInstanceOf(InvalidInvitationRoleException.class);
	}

	@Test
	void 이미_워크스페이스_구성원인_이메일이면_예외() {
		var request = new InvitationCreateRequest("member@growtech.io", Role.MEMBER);
		when(userRepository.existsByEmail("member@growtech.io")).thenReturn(true);

		assertThatThrownBy(() -> invitationService.createInvitation(adminPrincipal, request))
				.isInstanceOf(DuplicateMemberException.class);
	}

	@Test
	void 이미_PENDING_초대가_존재하면_예외() throws Exception {
		var request = new InvitationCreateRequest("newbie@growtech.io", Role.MEMBER);
		when(userRepository.existsByEmail("newbie@growtech.io")).thenReturn(false);

		Invitation existing = new Invitation(null, "newbie@growtech.io", Role.MEMBER, "existing-token", null,
				LocalDateTime.now().plusDays(7));
		when(invitationRepository.findByWorkspaceIdAndEmailAndStatus(10L, "newbie@growtech.io",
				InvitationStatus.PENDING)).thenReturn(java.util.Optional.of(existing));

		assertThatThrownBy(() -> invitationService.createInvitation(adminPrincipal, request))
				.isInstanceOf(DuplicateInvitationException.class);
	}
}
