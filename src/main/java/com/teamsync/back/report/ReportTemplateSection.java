package com.teamsync.back.report;

import com.teamsync.back.common.BaseTimeEntity;
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
 * FR-405 템플릿에 속한 섹션 한 건. sectionKey!=MANUAL이면 autoFilled=true로 고정(표준 섹션),
 * MANUAL이면 autoFilled=false로 고정(자유 섹션)한다 — 생성자에서만 결정하고 이후 sectionKey 자체는
 * 불변(제목만 PATCH 가능, 계약 문서 명시).
 */
@Getter
@Entity
@Table(name = "report_template_sections")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportTemplateSection extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_id", nullable = false)
	private ReportTemplate template;

	@Enumerated(EnumType.STRING)
	@Column(name = "section_key", nullable = false, length = 20)
	private ReportSectionKey sectionKey;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(name = "order_index", nullable = false)
	private int orderIndex;

	@Column(name = "auto_filled", nullable = false)
	private boolean autoFilled;

	public ReportTemplateSection(ReportTemplate template, ReportSectionKey sectionKey, String title, int orderIndex) {
		this.template = template;
		this.sectionKey = sectionKey;
		this.title = title;
		this.orderIndex = orderIndex;
		this.autoFilled = sectionKey != ReportSectionKey.MANUAL;
	}

	public void changeTitle(String title) {
		this.title = title;
	}

	public void changeOrderIndex(int orderIndex) {
		this.orderIndex = orderIndex;
	}
}
