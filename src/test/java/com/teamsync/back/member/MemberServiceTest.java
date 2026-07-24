package com.teamsync.back.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.LastActiveAdminException;
import com.teamsync.back.common.exception.LastAdminDemotionException;
import com.teamsync.back.common.exception.MemberNotFoundException;
import com.teamsync.back.common.exception.SelfDeactivationException;
import com.teamsync.back.invitation.InvitationRepository;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.user.UserStatus;
import com.teamsync.back.workspace.Workspace;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 구성원 관리(P1) 핵심 가드레일 단위 테스트: 유일한 ADMIN 강등/비활성화 방지, 자기 자신 비활성화 방지.
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private InvitationRepository invitationRepository;

	private MemberService memberService;
	private Workspace workspace;
	private AuthenticatedUser adminPrincipal;

	@BeforeEach
	void setUp() throws Exception {
		memberService = new MemberService(userRepository, invitationRepository);
		workspace = new Workspace("그로우테크", "growtech.io");
		setId(workspace, 10L);
		adminPrincipal = new AuthenticatedUser(1L, 10L, "admin@growtech.io", Role.ADMIN);
	}

	@Test
	void 유일한_ADMIN을_강등시키려_하면_예외() throws Exception {
		User admin = newUser("admin@growtech.io", "관리자", Role.ADMIN);
		setId(admin, 1L);
		when(userRepository.findByIdAndWorkspaceId(1L, 10L)).thenReturn(java.util.Optional.of(admin));
		when(userRepository.countByWorkspaceIdAndRole(10L, Role.ADMIN)).thenReturn(1L);

		assertThatThrownBy(() -> memberService.changeRole(adminPrincipal, 1L, Role.MEMBER))
				.isInstanceOf(LastAdminDemotionException.class);
	}

	@Test
	void ADMIN이_두명_이상이면_강등이_허용된다() throws Exception {
		User admin = newUser("admin@growtech.io", "관리자", Role.ADMIN);
		setId(admin, 1L);
		when(userRepository.findByIdAndWorkspaceId(1L, 10L)).thenReturn(java.util.Optional.of(admin));
		when(userRepository.countByWorkspaceIdAndRole(10L, Role.ADMIN)).thenReturn(2L);

		var response = memberService.changeRole(adminPrincipal, 1L, Role.LEADER);

		assertThat(response.role()).isEqualTo("LEADER");
	}

	@Test
	void 본인_계정을_비활성화하려_하면_예외() throws Exception {
		User admin = newUser("admin@growtech.io", "관리자", Role.ADMIN);
		setId(admin, 1L);
		when(userRepository.findByIdAndWorkspaceId(1L, 10L)).thenReturn(java.util.Optional.of(admin));

		assertThatThrownBy(() -> memberService.changeStatus(adminPrincipal, 1L, UserStatus.DEACTIVATED))
				.isInstanceOf(SelfDeactivationException.class);
	}

	@Test
	void 유일한_활성_ADMIN을_비활성화하려_하면_예외() throws Exception {
		User otherAdmin = newUser("other-admin@growtech.io", "다른관리자", Role.ADMIN);
		setId(otherAdmin, 2L);
		when(userRepository.findByIdAndWorkspaceId(2L, 10L)).thenReturn(java.util.Optional.of(otherAdmin));
		when(userRepository.countByWorkspaceIdAndRoleAndStatus(10L, Role.ADMIN, UserStatus.ACTIVE)).thenReturn(1L);

		assertThatThrownBy(() -> memberService.changeStatus(adminPrincipal, 2L, UserStatus.DEACTIVATED))
				.isInstanceOf(LastActiveAdminException.class);
	}

	@Test
	void 대상_구성원이_없으면_예외() {
		when(userRepository.findByIdAndWorkspaceId(999L, 10L)).thenReturn(java.util.Optional.empty());

		assertThatThrownBy(() -> memberService.changeRole(adminPrincipal, 999L, Role.MEMBER))
				.isInstanceOf(MemberNotFoundException.class);
	}

	private User newUser(String email, String name, Role role) {
		User user = new User(workspace, email, "hashed", name, role);
		return user;
	}

	private void setId(Object entity, Long id) throws Exception {
		Field idField = entity.getClass().getDeclaredField("id");
		idField.setAccessible(true);
		idField.set(entity, id);
	}
}
