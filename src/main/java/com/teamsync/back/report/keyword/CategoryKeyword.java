package com.teamsync.back.report.keyword;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주간 보고(V23) 대/중분류 표준 코드 테이블. 워크스페이스에 인증된 사용자라면 누구나 추가/이름변경/
 * 비활성화할 수 있다(관리자 전용 아님, CategoryKeywordController 참고). createdBy가 null이면 V23
 * 마이그레이션이 심은 표준 시드 항목이다. 삭제는 물리 삭제가 아니라 active=false로 숨김 처리해
 * 과거 WeeklyReportEntry의 FK 참조 무결성을 깨지 않는다(과거 이력에는 영향 없음).
 */
@Getter
@Entity
@Table(name = "category_keywords")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryKeyword extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private CategoryType type;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "order_index", nullable = false)
	private int orderIndex;

	@Column(nullable = false)
	private boolean active;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	public CategoryKeyword(CategoryType type, String name, int orderIndex, User createdBy) {
		this.type = type;
		this.name = name;
		this.orderIndex = orderIndex;
		this.active = true;
		this.createdBy = createdBy;
	}

	public void rename(String name) {
		this.name = name;
	}

	public void deactivate() {
		this.active = false;
	}
}
