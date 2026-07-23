package com.teamsync.back.task;

import com.teamsync.back.common.BaseTimeEntity;
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
 * FR-102: 태스크 카드의 체크리스트(서브태스크) 항목.
 */
@Getter
@Entity
@Table(name = "task_checklist_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskChecklistItem extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id", nullable = false)
	private Task task;

	@Column(nullable = false, length = 500)
	private String content;

	@Column(name = "is_checked", nullable = false)
	private boolean checked;

	@Column(nullable = false)
	private int position;

	public TaskChecklistItem(Task task, String content, int position) {
		this.task = task;
		this.content = content;
		this.checked = false;
		this.position = position;
	}

	public void changeContent(String content) {
		this.content = content;
	}

	public void changeChecked(boolean checked) {
		this.checked = checked;
	}
}
