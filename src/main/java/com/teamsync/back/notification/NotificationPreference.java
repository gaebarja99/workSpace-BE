package com.teamsync.back.notification;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.user.User;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-003 알림 세분화 설정: 사용자별 (카테고리 x 채널) 토글 1행. 사용자가 실제로 변경한 카테고리만 저장되며,
 * 저장된 적 없는 카테고리는 서비스에서 {@link NotificationCategory#defaults()}로 채워 응답한다.
 * (user, category)는 유니크하므로 PUT 부분 upsert의 충돌 판정 기준이 된다.
 */
@Getter
@Entity
@Table(name = "notification_preferences",
		uniqueConstraints = @UniqueConstraint(name = "uk_notification_preferences_user_category",
				columnNames = {"user_id", "category"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreference extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationCategory category;

	@Column(name = "in_app", nullable = false)
	private boolean inApp;

	@Column(nullable = false)
	private boolean email;

	@Column(nullable = false)
	private boolean push;

	public NotificationPreference(User user, NotificationCategory category, boolean inApp, boolean email, boolean push) {
		this.user = user;
		this.category = category;
		this.inApp = inApp;
		this.email = email;
		this.push = push;
	}

	/** PUT 부분 갱신 시 기존 행의 채널 값을 덮어쓴다. */
	public void update(boolean inApp, boolean email, boolean push) {
		this.inApp = inApp;
		this.email = email;
		this.push = push;
	}

	public EffectiveChannels toChannels() {
		return new EffectiveChannels(inApp, email, push);
	}
}
