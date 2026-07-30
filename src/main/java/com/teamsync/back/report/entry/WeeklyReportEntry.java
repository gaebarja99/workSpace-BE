package com.teamsync.back.report.entry;

import com.teamsync.back.report.WeeklyReport;
import com.teamsync.back.report.keyword.CategoryKeyword;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.project.Project;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주간 보고서(V28) 한 행: 대분류(실제 {@link Project} 참조)/중분류(표준 코드, {@link CategoryKeyword})
 * + 소분류/상세업무(자유 텍스트) + 달성율(0~100). {@link WeeklyReportService#replaceEntries}가 section
 * 단위로 기존 행을 통째로 지우고 요청 리스트로 다시 만드는 방식이라, 이 엔티티는 개별 행 수정 메서드 없이
 * 생성만 한다.
 */
@Getter
@Entity
@Table(name = "weekly_report_entries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyReportEntry extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "report_id", nullable = false)
	private WeeklyReport report;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EntrySection section;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "middle_category_id", nullable = false)
	private CategoryKeyword middleCategory;

	@Column(name = "minor_category", nullable = false, length = 255)
	private String minorCategory;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String detail;

	@Column(name = "rate_percent", nullable = false, columnDefinition = "SMALLINT")
	@JdbcTypeCode(SqlTypes.SMALLINT)
	private int ratePercent;

	@Column(name = "order_index", nullable = false)
	private int orderIndex;

	public WeeklyReportEntry(WeeklyReport report, EntrySection section, Project project,
			CategoryKeyword middleCategory, String minorCategory, String detail, int ratePercent, int orderIndex) {
		this.report = report;
		this.section = section;
		this.project = project;
		this.middleCategory = middleCategory;
		this.minorCategory = minorCategory != null ? minorCategory : "";
		this.detail = detail != null ? detail : "";
		this.ratePercent = ratePercent;
		this.orderIndex = orderIndex;
	}
}
