package com.teamsync.back.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.InvalidNotificationPreferenceException;
import com.teamsync.back.notification.dto.NotificationPreferenceView;
import com.teamsync.back.notification.dto.NotificationPreferencesResponse;
import com.teamsync.back.notification.dto.UpdateNotificationPreferencesRequest;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * FR-003 알림 세분화 설정의 핵심 로직(기본값 매트릭스 보강 / 부분 upsert / 검증)을 DB·스프링 컨텍스트 없이
 * 검증하는 단위 테스트. Repository는 Mockito로 대체한다(기존 SsoStateServiceTest/JwtTokenProviderTest 컨벤션).
 */
class NotificationPreferenceServiceTest {

	private NotificationPreferenceRepository preferenceRepository;
	private UserRepository userRepository;
	private NotificationPreferenceService service;

	private final AuthenticatedUser principal = new AuthenticatedUser(1L, 10L, "member@growtech.io", Role.MEMBER);

	@BeforeEach
	void setUp() {
		preferenceRepository = Mockito.mock(NotificationPreferenceRepository.class);
		userRepository = Mockito.mock(UserRepository.class);
		service = new NotificationPreferenceService(preferenceRepository, userRepository);
	}

	@Test
	void 저장값이_없으면_기본값_매트릭스로_5개_카테고리를_순서대로_반환한다() {
		when(preferenceRepository.findAllByUser_Id(1L)).thenReturn(List.of());

		NotificationPreferencesResponse response = service.getMyPreferences(principal);

		List<NotificationPreferenceView> categories = response.categories();
		assertThat(categories).extracting(NotificationPreferenceView::category)
				.containsExactly(
						NotificationCategory.TASK_DEADLINE,
						NotificationCategory.MENTION,
						NotificationCategory.TASK_ASSIGNED,
						NotificationCategory.TASK_STATUS_CHANGED,
						NotificationCategory.WEEKLY_REPORT);
		// 계약 기본값 매트릭스 그대로
		assertThat(categories).extracting(
						NotificationPreferenceView::inApp,
						NotificationPreferenceView::email,
						NotificationPreferenceView::push)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(true, true, false),   // TASK_DEADLINE
						org.assertj.core.groups.Tuple.tuple(true, true, true),    // MENTION
						org.assertj.core.groups.Tuple.tuple(true, false, false),  // TASK_ASSIGNED
						org.assertj.core.groups.Tuple.tuple(true, false, false),  // TASK_STATUS_CHANGED
						org.assertj.core.groups.Tuple.tuple(true, true, false));  // WEEKLY_REPORT
	}

	@Test
	void 저장값이_있는_카테고리는_저장값으로_나머지는_기본값으로_채운다() {
		NotificationPreference storedMention =
				new NotificationPreference(null, NotificationCategory.MENTION, false, false, false);
		when(preferenceRepository.findAllByUser_Id(1L)).thenReturn(List.of(storedMention));

		NotificationPreferencesResponse response = service.getMyPreferences(principal);

		NotificationPreferenceView mention = response.categories().stream()
				.filter(view -> view.category() == NotificationCategory.MENTION)
				.findFirst().orElseThrow();
		assertThat(mention.inApp()).isFalse();
		assertThat(mention.email()).isFalse();
		assertThat(mention.push()).isFalse();

		// 저장한 적 없는 TASK_DEADLINE은 기본값(t,t,f) 유지
		NotificationPreferenceView deadline = response.categories().stream()
				.filter(view -> view.category() == NotificationCategory.TASK_DEADLINE)
				.findFirst().orElseThrow();
		assertThat(deadline.inApp()).isTrue();
		assertThat(deadline.email()).isTrue();
		assertThat(deadline.push()).isFalse();
	}

	@Test
	void PUT은_본문에_포함된_카테고리만_upsert하고_누락_채널필드는_기본값으로_채운다() {
		// MENTION만, push=true 지정하고 inApp/email은 누락(null) → 기본값(t,t) 보강 기대
		UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest(List.of(
				new UpdateNotificationPreferencesRequest.Item("MENTION", null, null, true)));
		when(preferenceRepository.findByUser_IdAndCategory(1L, NotificationCategory.MENTION))
				.thenReturn(Optional.empty());
		when(userRepository.getReferenceById(1L)).thenReturn(Mockito.mock(User.class));
		when(preferenceRepository.findAllByUser_Id(1L)).thenReturn(List.of());

		service.updateMyPreferences(principal, request);

		ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
		verify(preferenceRepository).save(captor.capture());
		NotificationPreference saved = captor.getValue();
		assertThat(saved.getCategory()).isEqualTo(NotificationCategory.MENTION);
		assertThat(saved.isInApp()).isTrue();   // 기본값 보강
		assertThat(saved.isEmail()).isTrue();    // 기본값 보강
		assertThat(saved.isPush()).isTrue();     // 요청값
	}

	@Test
	void PUT은_이미_저장된_카테고리면_기존_행을_갱신하고_새로_저장하지_않는다() {
		NotificationPreference existing =
				new NotificationPreference(null, NotificationCategory.MENTION, true, true, true);
		UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest(List.of(
				new UpdateNotificationPreferencesRequest.Item("MENTION", true, false, false)));
		when(preferenceRepository.findByUser_IdAndCategory(1L, NotificationCategory.MENTION))
				.thenReturn(Optional.of(existing));
		when(preferenceRepository.findAllByUser_Id(1L)).thenReturn(List.of(existing));

		service.updateMyPreferences(principal, request);

		assertThat(existing.isInApp()).isTrue();
		assertThat(existing.isEmail()).isFalse();
		assertThat(existing.isPush()).isFalse();
		verify(preferenceRepository, never()).save(any());
	}

	@Test
	void PUT_알수없는_category는_400() {
		UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest(List.of(
				new UpdateNotificationPreferencesRequest.Item("UNKNOWN_CATEGORY", true, true, true)));

		assertThatThrownBy(() -> service.updateMyPreferences(principal, request))
				.isInstanceOf(InvalidNotificationPreferenceException.class);
		verify(preferenceRepository, never()).save(any());
	}

	@Test
	void PUT_categories_누락은_400() {
		UpdateNotificationPreferencesRequest request = new UpdateNotificationPreferencesRequest(null);

		assertThatThrownBy(() -> service.updateMyPreferences(principal, request))
				.isInstanceOf(InvalidNotificationPreferenceException.class);
	}

	@Test
	void resolve는_저장값_없는_수신자를_카테고리_기본값으로_채워_모든_userId를_반환한다() {
		NotificationPreference stored =
				new NotificationPreference(userWithId(2L), NotificationCategory.MENTION, false, false, false);
		when(preferenceRepository.findAllByCategoryAndUser_IdIn(eq(NotificationCategory.MENTION), any()))
				.thenReturn(List.of(stored));

		var result = service.resolve(NotificationCategory.MENTION, List.of(2L, 3L));

		// 저장값 있는 2L은 저장값(f,f,f), 없는 3L은 MENTION 기본값(t,t,t)
		assertThat(result.get(2L)).isEqualTo(new EffectiveChannels(false, false, false));
		assertThat(result.get(3L)).isEqualTo(new EffectiveChannels(true, true, true));
	}

	private static User userWithId(long id) {
		User user = Mockito.mock(User.class);
		when(user.getId()).thenReturn(id);
		return user;
	}
}
