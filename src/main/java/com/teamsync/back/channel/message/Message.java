package com.teamsync.back.channel.message;

import com.teamsync.back.channel.Channel;
import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

	// FR-402(주간 보고 하이라이트): pinned와 별개의 "개인 큐레이션" 성격이라 ADMIN/LEADER 전용인 pin과 달리
	// 메시지 작성 권한과 동일한 ADMIN/LEADER/MEMBER 누구나 토글할 수 있다(GUEST 제외). 주간 보고서(WeeklyReport)의
	// 하이라이트 섹션은 project+weekStart~weekEnd 범위로 이 필드를 실시간 조회해 구성한다(V10 마이그레이션).
	@Column(nullable = false)
	private boolean highlighted = false;

	@Column(name = "highlighted_at")
	private LocalDateTime highlightedAt;

	// FR-202-A(메시지 @멘션): 이 메시지에서 개별 언급된(워크스페이스 소속) 사용자. mentionEveryone(@전체)과 공존 가능.
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "message_mentions",
			joinColumns = @JoinColumn(name = "message_id"),
			inverseJoinColumns = @JoinColumn(name = "user_id"))
	private Set<User> mentionedUsers = new LinkedHashSet<>();

	@Column(name = "mention_everyone", nullable = false)
	private boolean mentionEveryone = false;

	// FR-202-B(메시지 이모지 반응): 메시지 삭제 시 반응도 함께 정리된다(cascade + orphanRemoval).
	@OneToMany(mappedBy = "message", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<MessageReaction> reactions = new ArrayList<>();

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
	 * FR-202-A: 메시지 생성 직후 멘션 정보를 채운다(개별 멘션 사용자 집합 + @전체 여부).
	 * USER 메시지에만 적용되며, SYSTEM/TASK_COMMENT_SYNC 메시지는 호출하지 않으므로 빈 상태를 유지한다.
	 */
	public void applyMentions(Set<User> mentionedUsers, boolean mentionEveryone) {
		this.mentionedUsers = new LinkedHashSet<>(mentionedUsers);
		this.mentionEveryone = mentionEveryone;
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

	/**
	 * FR-402: "이번 주 하이라이트로 지정" 토글. ADMIN/LEADER/MEMBER 누구나 호출 가능(컨트롤러
	 * @PreAuthorize에서 제어)하며, pin과 달리 답글에 대한 제한을 두지 않는다(개인 큐레이션 성격).
	 */
	public void highlight() {
		this.highlighted = true;
		this.highlightedAt = LocalDateTime.now();
	}

	public void unhighlight() {
		this.highlighted = false;
		this.highlightedAt = null;
	}
}
