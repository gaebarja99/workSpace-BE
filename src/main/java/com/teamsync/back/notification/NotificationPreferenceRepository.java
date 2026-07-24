package com.teamsync.back.notification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

	// GET /api/notifications/preferences: 본인이 저장한 모든 카테고리 설정(미저장 카테고리는 서비스에서 기본값 보강).
	List<NotificationPreference> findAllByUser_Id(Long userId);

	// PUT 부분 upsert: (user, category) 유니크 기준으로 기존 행을 찾아 update, 없으면 새로 생성.
	Optional<NotificationPreference> findByUser_IdAndCategory(Long userId, NotificationCategory category);

	// 발송 훅의 N+1 방지: "특정 카테고리 + 수신자 집합"의 저장된 설정만 한 번에 조회한다.
	List<NotificationPreference> findAllByCategoryAndUser_IdIn(NotificationCategory category, Collection<Long> userIds);
}
