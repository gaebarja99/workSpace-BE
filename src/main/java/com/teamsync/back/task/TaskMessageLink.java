package com.teamsync.back.task;

import com.teamsync.back.channel.message.Message;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * FR-301/303(메시지↔태스크 연동, US-09): 채널 메시지를 태스크로 전환할 때 생성되는 단방향 참조.
 * 태스크당 최대 1개(task_id UNIQUE, V8 마이그레이션)만 존재하며, "관련 대화 보기"(FR-303)에서
 * 태스크가 원본 메시지(=연결된 스레드의 루트)를 조회하는 용도로만 쓰인다.
 * 트랜잭션 원칙(PRD 5.6): 이 엔티티는 messages 테이블을 참조만 할 뿐 어떤 컬럼도 변경하지 않으므로,
 * 변환 트랜잭션이 실패해도 원본 메시지는 항상 그대로 유지된다.
 */
@Getter
@Entity
@Table(name = "task_message_links")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskMessageLink {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id", nullable = false, unique = true)
	private Task task;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "message_id", nullable = false)
	private Message message;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public TaskMessageLink(Task task, Message message) {
		this.task = task;
		this.message = message;
	}
}
