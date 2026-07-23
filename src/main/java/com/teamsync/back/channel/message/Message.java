package com.teamsync.back.channel.message;

import com.teamsync.back.channel.Channel;
import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-202 채널 메시지 최소 골격(이번 범위는 REST + 프론트 폴링으로 근사, WebSocket 실시간 전송은 후속 과제).
 * parentMessage는 스레드 답글을 표현하는 자기 참조이며, 조회 API는 최상위/답글을 구분하지 않고
 * 평평한 리스트로 반환한다(그룹핑은 프론트엔드 책임). 원본 메시지는 항상 그대로 유지되어야 하므로
 * (5.6 리스크 대응, task_message_links 연동 전제) 이 엔티티에는 태스크 참조 필드를 두지 않는다.
 */
@Getter
@Entity
@Table(name = "messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "channel_id", nullable = false)
	private Channel channel;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_message_id")
	private Message parentMessage;

	@Column(nullable = false)
	private boolean pinned = false;

	@Column(name = "pinned_at")
	private LocalDateTime pinnedAt;

	public Message(Channel channel, User author, String content, Message parentMessage) {
		this.channel = channel;
		this.author = author;
		this.content = content;
		this.parentMessage = parentMessage;
	}

	/**
	 * FR-203(메시지 고정, US-07): 팀장/관리자가 공지성 메시지를 채널 상단에 고정한다.
	 */
	public void pin() {
		this.pinned = true;
		this.pinnedAt = LocalDateTime.now();
	}

	public void unpin() {
		this.pinned = false;
		this.pinnedAt = null;
	}
}
