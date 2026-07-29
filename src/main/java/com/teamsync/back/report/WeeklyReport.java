package com.teamsync.back.report;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.user.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-401~404(개인 주간 보고서, V23 재설계). project 종속을 제거하고 user_id + week_start 단위로
 * 한 사람당 한 주에 하나만 존재한다(V23 유니크 제약) — 한 사람이 한 주에 여러 프로젝트(대분류) 업무를
 * 섞어 적을 수 있어야 하기 때문이다. 완료/진행/이슈를 Task에서 자동 계산하던 방식은 완전히 폐기하고,
 * 사용자가 {@link WeeklyReportEntry}(대/중/소분류 + 상세업무 + 달성율) 행을 직접 추가하는 방식으로
 * 대체한다(WeeklyReportService 참고).
 */
@Getter
@Entity
@Table(name = "weekly_reports",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_weekly_reports_user_week",
				columnNames = {"user_id", "week_start"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyReport extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "week_start", nullable = false)
	private LocalDate weekStart;

	@Column(name = "week_end", nullable = false)
	private LocalDate weekEnd;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WeeklyReportStatus status;

	@Column(name = "submitted_at")
	private LocalDateTime submittedAt;

	@OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("section ASC, orderIndex ASC")
	private List<WeeklyReportEntry> entries = new ArrayList<>();

	public WeeklyReport(User user, LocalDate weekStart, LocalDate weekEnd) {
		this.user = user;
		this.weekStart = weekStart;
		this.weekEnd = weekEnd;
		this.status = WeeklyReportStatus.DRAFT;
	}

	/**
	 * FR-403: DRAFT -> SUBMITTED 전이. 이미 SUBMITTED인 경우의 재제출 차단은 서비스 계층에서
	 * WeeklyReportAlreadySubmittedException으로 처리하고, 이 메서드는 상태 전이만 담당한다.
	 */
	public void submit() {
		this.status = WeeklyReportStatus.SUBMITTED;
		this.submittedAt = LocalDateTime.now();
	}

	/**
	 * SUBMITTED -> DRAFT 재전이("다시 작성하기"). 제출 후 실수를 고칠 수 있도록 원본 불변 원칙을
	 * 사용자 의지로 되돌리는 유일한 경로다. submittedAt은 지워서 재제출 시 다시 채워지게 한다.
	 */
	public void reopen() {
		this.status = WeeklyReportStatus.DRAFT;
		this.submittedAt = null;
	}
}
