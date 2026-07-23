package com.teamsync.back.report;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.project.Project;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-404(팀 주간 보고 발행 기록). project+weekStart 단위로 존재 여부만으로 "발행 완료"(레코드 있음)
 * vs "집계 중"(레코드 없음)을 판정한다(V10 유니크 제약). 집계 내용(팀 완료/이슈 건수 등) 자체는 이
 * 레코드에 스냅샷하지 않고 GET /reports/team 호출 시 WeeklyReportService가 항상 실시간 계산한다
 * (개인 WeeklyReport와 동일한 이유: 스냅샷 도입 없이 주차 범위 실시간 쿼리로 충분).
 */
@Getter
@Entity
@Table(name = "team_weekly_reports",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_team_weekly_reports_project_week",
				columnNames = {"project_id", "week_start"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamWeeklyReport extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(name = "week_start", nullable = false)
	private LocalDate weekStart;

	@Column(name = "week_end", nullable = false)
	private LocalDate weekEnd;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "published_by", nullable = false)
	private User publishedBy;

	@Column(name = "published_at", nullable = false)
	private LocalDateTime publishedAt;

	public TeamWeeklyReport(Project project, LocalDate weekStart, LocalDate weekEnd, User publishedBy) {
		this.project = project;
		this.weekStart = weekStart;
		this.weekEnd = weekEnd;
		this.publishedBy = publishedBy;
		this.publishedAt = LocalDateTime.now();
	}

	/**
	 * FR-404: "같은 주 재발행 시 publishedBy/At 갱신"(계약 문서) — upsert의 update 분기에서 사용.
	 */
	public void republish(User publishedBy) {
		this.publishedBy = publishedBy;
		this.publishedAt = LocalDateTime.now();
	}
}
