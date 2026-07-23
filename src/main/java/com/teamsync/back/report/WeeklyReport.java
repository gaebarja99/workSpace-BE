package com.teamsync.back.report;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.project.Project;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-401~404(개인 주간 보고서). project+user+weekStart 단위로 하나만 존재한다(V10 유니크 제약).
 * 완료/진행/하이라이트/이슈 섹션은 이 엔티티에 저장하지 않고 project+weekStart~weekEnd 범위로 항상
 * 실시간 쿼리로 계산한다(WeeklyReportService 참고). 이유: JSON 컬럼 도입 없이도 주차 범위가 고정이라
 * 실무상 충분히 안정적이고, FR-004 때처럼 불필요한 복잡도를 피하는 이 코드베이스의 기존 원칙과 맞기 때문.
 * 한계(문서화): 제출(SUBMITTED) 이후 원본 태스크 상태가 다시 바뀌면 과거 보고서의 자동 섹션 내용도 그에 따라
 * 바뀔 수 있다(스냅샷이 아니므로) — MVP 허용 범위로 남겨둔다.
 * updatedAt(BaseTimeEntity)이 곧 "마지막 자동 저장" 시각이다(PATCH /reports/me 호출 시마다 갱신).
 */
@Getter
@Entity
@Table(name = "weekly_reports",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_weekly_reports_project_user_week",
				columnNames = {"project_id", "user_id", "week_start"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyReport extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

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

	@Column(name = "next_week_plan", nullable = false, columnDefinition = "TEXT")
	private String nextWeekPlan;

	@Column(name = "submitted_at")
	private LocalDateTime submittedAt;

	public WeeklyReport(Project project, User user, LocalDate weekStart, LocalDate weekEnd) {
		this.project = project;
		this.user = user;
		this.weekStart = weekStart;
		this.weekEnd = weekEnd;
		this.status = WeeklyReportStatus.DRAFT;
		this.nextWeekPlan = "";
	}

	public void changeNextWeekPlan(String nextWeekPlan) {
		this.nextWeekPlan = nextWeekPlan != null ? nextWeekPlan : "";
	}

	/**
	 * FR-403: DRAFT -> SUBMITTED 전이. 이미 SUBMITTED인 경우의 재제출 차단은 서비스 계층에서
	 * WeeklyReportAlreadySubmittedException으로 처리하고, 이 메서드는 상태 전이만 담당한다.
	 */
	public void submit() {
		this.status = WeeklyReportStatus.SUBMITTED;
		this.submittedAt = LocalDateTime.now();
	}
}
