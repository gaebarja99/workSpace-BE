package com.teamsync.back.channel.message;

import com.teamsync.back.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * FR-202-B(메시지 이모지 반응): 메시지에 대한 사용자별 이모지 반응. (message_id, user_id, emoji) 조합은
 * 유일하므로(한 유저가 같은 메시지에 같은 이모지를 두 번 남길 수 없음) UNIQUE 제약으로 보장하고,
 * 토글 시 이미 존재하면 삭제·없으면 추가한다(ChannelService.toggleReaction).
 */
@Getter
@Entity
@Table(name = "message_reactions", uniqueConstraints = @UniqueConstraint(
		name = "uk_message_reactions_message_user_emoji",
		columnNames = {"message_id", "user_id", "emoji"}))
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageReaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "message_id", nullable = false)
	private Message message;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 16)
	private String emoji;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public MessageReaction(Message message, User user, String emoji) {
		this.message = message;
		this.user = user;
		this.emoji = emoji;
	}
}
