package com.teamsync.back.archive;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.project.Project;
import com.teamsync.back.user.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-205 정보 아카이브(위키형 결정사항/참고자료). 순수 텍스트 콘텐츠만 다루며 별도 인프라
 * 의존이 없다. author는 nullable로 두어(요구사항 명시) 작성자 계정이 이후 삭제되는 등의 경우에도
 * 항목 자체는 유지될 수 있게 한다. tags는 자유 키워드 목록으로, 콤마 이스케이프 문제를 피하기
 * 위해 콤마 구분 컬럼 대신 순서를 보존하는 별도 컬렉션 테이블(archive_item_tags)에 저장한다.
 */
@Getter
@Entity
@Table(name = "archive_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArchiveItem extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ArchiveItemType type;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id")
	private User author;

	@ElementCollection
	@CollectionTable(name = "archive_item_tags", joinColumns = @JoinColumn(name = "archive_item_id"))
	@OrderColumn(name = "position")
	@Column(name = "tag", nullable = false, length = 100)
	private List<String> tags = new ArrayList<>();

	public ArchiveItem(Project project, ArchiveItemType type, String title, String content, User author,
			List<String> tags) {
		this.project = project;
		this.type = type;
		this.title = title;
		this.content = content;
		this.author = author;
		this.tags = new ArrayList<>(tags);
	}
}
