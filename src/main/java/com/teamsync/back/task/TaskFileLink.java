package com.teamsync.back.task;

import com.teamsync.back.archive.file.ArchivedFile;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * FR-304(US-09): 채널/아카이브에 업로드된 파일을 재업로드 없이 태스크 카드에 "링크"만 하는 다대다 연결.
 * 파일 자체(ArchivedFile)는 변경/복사하지 않고 참조만 하며, 링크 삭제는 이 연결만 제거할 뿐
 * 원본 파일에는 영향을 주지 않는다.
 */
@Getter
@Entity
@Table(name = "task_file_links")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskFileLink {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id", nullable = false)
	private Task task;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "archived_file_id", nullable = false)
	private ArchivedFile archivedFile;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "linked_by", nullable = false)
	private User linkedBy;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public TaskFileLink(Task task, ArchivedFile archivedFile, User linkedBy) {
		this.task = task;
		this.archivedFile = archivedFile;
		this.linkedBy = linkedBy;
	}
}
