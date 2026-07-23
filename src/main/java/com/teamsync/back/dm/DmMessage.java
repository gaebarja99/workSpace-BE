package com.teamsync.back.dm;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-206 DM 메시지. 채널 메시지(Message)와 달리 스레드(parentMessage)·고정(pinned)·messageType이 없다
 * (DM에는 SYSTEM 메시지가 없고 스코프도 아니다, 계약 문서 fr206-contract.md 참고). author는 항상 NOT NULL이다.
 */
@Getter
@Entity
@Table(name = "dm_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DmMessage extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "conversation_id", nullable = false)
	private DmConversation conversation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	public DmMessage(DmConversation conversation, User author, String content) {
		this.conversation = conversation;
		this.author = author;
		this.content = content;
	}
}
