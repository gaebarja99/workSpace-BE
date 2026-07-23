package com.teamsync.back.channel.message;

import com.teamsync.back.channel.Channel;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-202 채널 메시지 최소 골격(이번 범위는 REST + 프론트 폴링으로 근사, WebSocket 실시간 전송은 후속 과제).
 * parentMessage는 스레드 답글을 표현하는 자기 참조이며, 조회 API는 최상위/답글을 구분하지 않고
 * 평평한 리스트로 반환한다(그룹핑은 프론트엔드 책임).
 * FR-301~305(메시지↔태스크 연동, US-09/10): 원본 메시지는 항상 그대로 유지되어야 하므로(5.6 리스크 대응)
 * 이 엔티티에는 태스크 참조 필드를 두지 않고, task_message_links(TaskMessageLink)가 단방향으로
 * 이 엔티티를 참조한다. FR-302(시스템 메시지)는 author 없이 게시되므로 author는 nullable이며,
 * messageType(USER/SYSTEM/TASK_COMMENT_SYNC)으로 출처를 구분한다.
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

	// FR-302: 시스템 메시지는 사람이 작성하지 않으므로 nullable(=author_id NULL 허용, V8 마이그레이션).
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id")
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

	@Enumerated(EnumType.STRING)
	@Column(name = "message_type", nullable = false, length = 20)
	private MessageType messageType = MessageType.USER;

	public Message(Channel channel, User author, String content, Message parentMessage) {
		this(channel, author, content, parentMessage, MessageType.USER);
	}

	private Message(Channel channel, User author, String content, Message parentMessage, MessageType messageType) {
		this.channel = channel;
		this.author = author;
		this.content = content;
		this.parentMessage = parentMessage;
		this.messageType = messageType;
	}

	/**
	 * FR-302: 태스크 생성/상태변경/완료 시 서버가 자동 게시하는 시스템 메시지. author는 항상 null이다.
	 */
	public static Message createSystemMessage(Channel channel, String content, Message parentMessage) {
		return new Message(channel, null, content, parentMessage, MessageType.SYSTEM);
	}

	/**
	 * FR-305: 태스크 댓글 작성 시 연결된 채널 스레드로 동일 내용을 동기화하는 메시지.
	 * author는 댓글을 작성한 사람 그대로 채운다(뱃지 표시로만 SYSTEM과 구분).
	 */
	public static Message createTaskCommentSync(Channel channel, User author, String content, Message parentMessage) {
		return new Message(channel, author, content, parentMessage, MessageType.TASK_COMMENT_SYNC);
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
