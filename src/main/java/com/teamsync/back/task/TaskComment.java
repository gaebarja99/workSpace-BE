package com.teamsync.back.task;

import com.teamsync.back.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * FR-305(US-10): 태스크 댓글. 작성 시(같은 트랜잭션 내에서) 태스크가 task_message_links로 채널 메시지에
 * 연결되어 있으면 그 채널 스레드에도 동일 내용이 TASK_COMMENT_SYNC 메시지로 함께 게시된다
 * (TaskService.createTaskComment 참고). 연결이 없으면 댓글만 저장되고 에러가 아니다.
 */
@Getter
@Entity
@Table(name = "task_comments")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskComment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id", nullable = false)
	private Task task;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	// FR-105-A: 이 댓글에서 언급된(워크스페이스 소속) 사용자. Task.assignees와 동일하게 단방향 ManyToMany로 관리한다.
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "task_comment_mentions",
			joinColumns = @JoinColumn(name = "task_comment_id"),
			inverseJoinColumns = @JoinColumn(name = "user_id"))
	private Set<User> mentionedUsers = new LinkedHashSet<>();

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public TaskComment(Task task, User author, String content, Set<User> mentionedUsers) {
		this.task = task;
		this.author = author;
		this.content = content;
		this.mentionedUsers = new LinkedHashSet<>(mentionedUsers);
	}
}
