package com.teamsync.back.notification;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.InvalidNotificationPreferenceException;
import com.teamsync.back.notification.dto.NotificationPreferenceView;
import com.teamsync.back.notification.dto.NotificationPreferencesResponse;
import com.teamsync.back.notification.dto.UpdateNotificationPreferencesRequest;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-003 알림 세분화 설정의 조회/수정(GET·PUT)과, 발송 훅이 쓰는 "카테고리별 유효 채널" 해석을 담당한다.
 * 저장된 설정이 없는 카테고리는 항상 {@link NotificationCategory#defaults()}로 채워, GET은 언제나 5개
 * 카테고리 전부를 계약 순서(enum 선언 순)대로 반환한다.
 */
@Service
public class NotificationPreferenceService {

	private final NotificationPreferenceRepository preferenceRepository;
	private final UserRepository userRepository;

	public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository,
			UserRepository userRepository) {
		this.preferenceRepository = preferenceRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public NotificationPreferencesResponse getMyPreferences(AuthenticatedUser principal) {
		Map<NotificationCategory, EffectiveChannels> stored = storedByCategory(principal.userId());
		return toResponse(stored);
	}

	/**
	 * PUT 부분 upsert: 본문에 포함된 카테고리만 갱신하고, 미포함 카테고리는 기존/기본값을 유지한다.
	 * categories 누락/알 수 없는 category는 400. 채널 필드 누락 시 해당 카테고리 기본값으로 간주한다.
	 */
	@Transactional
	public NotificationPreferencesResponse updateMyPreferences(AuthenticatedUser principal,
			UpdateNotificationPreferencesRequest request) {
		if (request == null || request.categories() == null) {
			throw new InvalidNotificationPreferenceException("categories 필드는 배열이어야 합니다.");
		}
		Long userId = principal.userId();
		for (UpdateNotificationPreferencesRequest.Item item : request.categories()) {
			NotificationCategory category = parseCategory(item.category());
			EffectiveChannels defaults = category.defaults();
			// 각 채널 필드가 누락(null)되면 기본값 매트릭스 값으로 간주한다.
			boolean inApp = item.inApp() != null ? item.inApp() : defaults.inApp();
			boolean email = item.email() != null ? item.email() : defaults.email();
			boolean push = item.push() != null ? item.push() : defaults.push();

			preferenceRepository.findByUser_IdAndCategory(userId, category)
					.ifPresentOrElse(
							existing -> existing.update(inApp, email, push),
							() -> {
								User userRef = userRepository.getReferenceById(userId);
								preferenceRepository.save(
										new NotificationPreference(userRef, category, inApp, email, push));
							});
		}
		return toResponse(storedByCategory(userId));
	}

	/**
	 * 발송 훅용: 특정 카테고리에 대해 요청한 수신자 전원의 유효 채널을 한 번의 조회로 해석한다(N+1 방지).
	 * 저장된 설정이 없는 수신자는 카테고리 기본값으로 채워 항상 요청한 모든 userId에 대한 값을 담아 돌려준다.
	 */
	@Transactional(readOnly = true)
	public Map<Long, EffectiveChannels> resolve(NotificationCategory category, Collection<Long> userIds) {
		Map<Long, EffectiveChannels> result = new HashMap<>();
		if (userIds.isEmpty()) {
			return result;
		}
		Map<Long, EffectiveChannels> stored = new HashMap<>();
		for (NotificationPreference pref : preferenceRepository.findAllByCategoryAndUser_IdIn(category, userIds)) {
			stored.put(pref.getUser().getId(), pref.toChannels());
		}
		EffectiveChannels defaults = category.defaults();
		for (Long userId : userIds) {
			result.put(userId, stored.getOrDefault(userId, defaults));
		}
		return result;
	}

	private Map<NotificationCategory, EffectiveChannels> storedByCategory(Long userId) {
		Map<NotificationCategory, EffectiveChannels> map = new EnumMap<>(NotificationCategory.class);
		for (NotificationPreference pref : preferenceRepository.findAllByUser_Id(userId)) {
			map.put(pref.getCategory(), pref.toChannels());
		}
		return map;
	}

	/** 저장된 값 위에 기본값을 덮어, 항상 5개 카테고리를 enum 선언 순서대로 담은 응답을 만든다. */
	private NotificationPreferencesResponse toResponse(Map<NotificationCategory, EffectiveChannels> stored) {
		List<NotificationPreferenceView> views = Arrays.stream(NotificationCategory.values())
				.map(category -> NotificationPreferenceView.of(category,
						stored.getOrDefault(category, category.defaults())))
				.toList();
		return new NotificationPreferencesResponse(views);
	}

	private NotificationCategory parseCategory(String raw) {
		if (raw == null) {
			throw new InvalidNotificationPreferenceException("category는 필수입니다.");
		}
		try {
			return NotificationCategory.valueOf(raw);
		} catch (IllegalArgumentException e) {
			throw new InvalidNotificationPreferenceException("알 수 없는 category입니다: " + raw);
		}
	}
}
