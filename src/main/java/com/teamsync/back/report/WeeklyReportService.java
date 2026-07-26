package com.teamsync.back.report;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.InvalidReportRequestException;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.common.exception.TeamWeeklyReportNotFoundException;
import com.teamsync.back.common.exception.WeeklyReportAlreadySubmittedException;
import com.teamsync.back.common.exception.WeeklyReportNotFoundException;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.project.ProjectStatus;
import com.teamsync.back.report.dto.CompletedTaskItem;
import com.teamsync.back.report.dto.InProgressTaskItem;
import com.teamsync.back.report.dto.IssueItem;
import com.teamsync.back.report.dto.IssueKind;
import com.teamsync.back.report.dto.MemberSubmissionStatus;
import com.teamsync.back.report.dto.NextWeekPlanUpdateRequest;
import com.teamsync.back.report.dto.ReportHistoryItem;
import com.teamsync.back.report.dto.ReportHistoryStatus;
import com.teamsync.back.report.dto.RollupResponse;
import com.teamsync.back.report.dto.RollupTeamItem;
import com.teamsync.back.report.dto.RollupTrendItem;
import com.teamsync.back.report.dto.TeamMemberReportSummary;
import com.teamsync.back.report.dto.TeamWeeklyReportExportView;
import com.teamsync.back.report.dto.TeamWeeklyReportResponse;
import com.teamsync.back.report.dto.WeeklyReportExportView;
import com.teamsync.back.report.dto.WeeklyReportResponse;
import com.teamsync.back.task.Task;
import com.teamsync.back.task.TaskRepository;
import com.teamsync.back.task.TaskStatus;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-401~404/409/410(P2 주간 보고 자동화 + P3 내보내기). 계약 문서(p2-weekly-report-contract.md)
 * 기준 원칙: 완료/진행/이슈 섹션은 WeeklyReport/TeamWeeklyReport 어디에도 스냅샷하지 않고
 * 매 요청마다 project(+user)+weekStart~weekEnd 범위로 실시간 계산한다. 계약 문서에 명시된
 * "3주 이상 정체(STALE)" 플래그만 FR-401 이슈 섹션의 축소판으로 포함한다. FR-409(보고서 내보내기)는
 * 이 서비스가 이미 만들어 둔 toResponse/buildTeamResponse 계산 결과를 그대로 재사용하고(별도
 * 스냅샷/재계산 로직을 새로 만들지 않음), reportId로 직접 조회하는 getReportForExport/getTeamReportForExport
 * 두 메서드만 추가한다(ReportExportService가 호출). 팀 채팅(하이라이트)/알림(미제출 리마인드) 기능 제거에 따라
 * 하이라이트 섹션과 리마인드(수동/자동 배치) 기능은 더 이상 지원하지 않는다.
 */
@Service
public class WeeklyReportService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int STALE_DAYS_THRESHOLD = 21;
	private static final int TOP_TITLES_LIMIT = 5;

	private final WeeklyReportRepository weeklyReportRepository;
	private final TeamWeeklyReportRepository teamWeeklyReportRepository;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final TaskRepository taskRepository;

	public WeeklyReportService(WeeklyReportRepository weeklyReportRepository,
			TeamWeeklyReportRepository teamWeeklyReportRepository, ProjectRepository projectRepository,
			UserRepository userRepository, TaskRepository taskRepository) {
		this.weeklyReportRepository = weeklyReportRepository;
		this.teamWeeklyReportRepository = teamWeeklyReportRepository;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
		this.taskRepository = taskRepository;
	}

	// ----- FR-401/403: 개인 보고서 -----

	@Transactional
	public WeeklyReportResponse getOrCreateMyReport(AuthenticatedUser principal, Long projectId, LocalDate weekStartParam) {
		Project project = getProjectInWorkspace(principal, projectId);
		LocalDate weekStart = resolveWeekStart(weekStartParam);
		User user = userRepository.getReferenceById(principal.userId());
		WeeklyReport report = getOrCreateEntity(project, user, weekStart);
		return toResponse(report);
	}

	@Transactional
	public WeeklyReportResponse updateNextWeekPlan(AuthenticatedUser principal, Long projectId,
			LocalDate weekStartParam, NextWeekPlanUpdateRequest request) {
		Project project = getProjectInWorkspace(principal, projectId);
		LocalDate weekStart = resolveWeekStart(weekStartParam);
		User user = userRepository.getReferenceById(principal.userId());
		WeeklyReport report = getOrCreateEntity(project, user, weekStart);
		if (report.getStatus() == WeeklyReportStatus.SUBMITTED) {
			throw new WeeklyReportAlreadySubmittedException();
		}
		report.changeNextWeekPlan(request.nextWeekPlan());
		return toResponse(report);
	}

	@Transactional
	public WeeklyReportResponse submitMyReport(AuthenticatedUser principal, Long projectId, LocalDate weekStartParam) {
		Project project = getProjectInWorkspace(principal, projectId);
		LocalDate weekStart = resolveWeekStart(weekStartParam);
		User user = userRepository.getReferenceById(principal.userId());
		WeeklyReport report = getOrCreateEntity(project, user, weekStart);
		if (report.getStatus() == WeeklyReportStatus.SUBMITTED) {
			throw new WeeklyReportAlreadySubmittedException();
		}
		report.submit();
		return toResponse(report);
	}

	// ----- FR-404: 팀 보고서 -----

	@Transactional
	public TeamWeeklyReportResponse getTeamReport(AuthenticatedUser principal, Long projectId, LocalDate weekStartParam) {
		Project project = getProjectInWorkspace(principal, projectId);
		LocalDate weekStart = resolveWeekStart(weekStartParam);
		return buildTeamResponse(project, weekStart);
	}

	@Transactional
	public TeamWeeklyReportResponse publishTeamReport(AuthenticatedUser principal, Long projectId,
			LocalDate weekStartParam) {
		Project project = getProjectInWorkspace(principal, projectId);
		LocalDate weekStart = resolveWeekStart(weekStartParam);
		LocalDate weekEnd = weekEndOf(weekStart);
		User publishedBy = userRepository.getReferenceById(principal.userId());

		teamWeeklyReportRepository.findByProject_IdAndWeekStart(projectId, weekStart)
				.ifPresentOrElse(
						existing -> existing.republish(publishedBy),
						() -> teamWeeklyReportRepository
								.save(new TeamWeeklyReport(project, weekStart, weekEnd, publishedBy)));

		return buildTeamResponse(project, weekStart);
	}

	// ----- FR-410: 보고 이력 -----

	@Transactional(readOnly = true)
	public List<ReportHistoryItem> getHistory(AuthenticatedUser principal, Long projectId, LocalDate weekStartParam,
			String q) {
		Project project = getProjectInWorkspace(principal, projectId);

		Set<LocalDate> allWeeks = new TreeSet<>(Comparator.reverseOrder());
		allWeeks.addAll(weeklyReportRepository.findDistinctWeekStartsByProjectId(projectId));
		allWeeks.addAll(teamWeeklyReportRepository.findDistinctWeekStartsByProjectId(projectId));

		List<LocalDate> candidateWeeks;
		if (weekStartParam != null) {
			LocalDate normalized = resolveWeekStart(weekStartParam);
			candidateWeeks = allWeeks.contains(normalized) ? List.of(normalized) : List.of();
		} else {
			candidateWeeks = new ArrayList<>(allWeeks);
		}

		String keyword = (q != null && !q.isBlank()) ? escapeLikeWildcards(q.trim()) : null;
		int totalMemberCount = reportMembers(principal.workspaceId()).size();

		List<ReportHistoryItem> result = new ArrayList<>();
		for (LocalDate weekStart : candidateWeeks) {
			LocalDate weekEnd = weekEndOf(weekStart);
			if (keyword != null && !matchesKeyword(projectId, weekStart, keyword)) {
				continue;
			}

			long submittedCount = weeklyReportRepository
					.countByProject_IdAndWeekStartAndStatus(projectId, weekStart, WeeklyReportStatus.SUBMITTED);
			boolean published = teamWeeklyReportRepository.findByProject_IdAndWeekStart(projectId, weekStart).isPresent();
			int issueCount = countTeamIssues(project, weekStart, weekEnd);
			double completionRate = totalMemberCount == 0 ? 0.0 : (double) submittedCount / totalMemberCount;

			result.add(new ReportHistoryItem(
					weekStart, weekEnd,
					published ? ReportHistoryStatus.PUBLISHED : ReportHistoryStatus.AGGREGATING,
					(int) submittedCount, totalMemberCount, completionRate, issueCount));
		}
		return result;
	}

	// ----- FR-409: 보고서 내보내기(PDF/이메일) -----

	/**
	 * reportId로 개인 주간 보고서를 직접 조회한다. GET /reports/me와 달리 "본인 소유가 아닌 보고서"도
	 * 조회 대상이 될 수 있어(계약: "본인 또는 같은 워크스페이스 LEADER/ADMIN만 허용") project+weekStart
	 * 기반 조회 대신 PK로 직접 찾는다. 워크스페이스 불일치·권한 부족 모두 동일하게 404로 응답해
	 * 리소스 존재 여부를 숨긴다(ReportTemplateNotFoundException 등과 동일 원칙, PRD 5.6).
	 */
	@Transactional(readOnly = true)
	public WeeklyReportExportView getReportForExport(AuthenticatedUser principal, Long reportId) {
		WeeklyReport report = weeklyReportRepository.findById(reportId)
				.filter(r -> r.getProject().getWorkspace().getId().equals(principal.workspaceId()))
				.orElseThrow(WeeklyReportNotFoundException::new);

		boolean isOwner = report.getUser().getId().equals(principal.userId());
		boolean isLeaderOrAdmin = principal.role() == Role.ADMIN || principal.role() == Role.LEADER;
		if (!isOwner && !isLeaderOrAdmin) {
			throw new WeeklyReportNotFoundException();
		}

		return new WeeklyReportExportView(toResponse(report), report.getProject().getName(),
				report.getUser().getName(), report.getUser().getEmail());
	}

	/**
	 * teamReportId(TeamWeeklyReport PK)로 팀 주간 보고를 직접 조회한다. LEADER/ADMIN 제한은
	 * 컨트롤러의 @PreAuthorize가 이미 담당하므로(GET /reports/team과 동일 패턴) 여기서는
	 * 워크스페이스 스코핑만 검증한다. 집계 내용은 buildTeamResponse로 그대로 재계산해 재사용한다.
	 */
	@Transactional(readOnly = true)
	public TeamWeeklyReportExportView getTeamReportForExport(AuthenticatedUser principal, Long teamReportId) {
		TeamWeeklyReport teamReport = teamWeeklyReportRepository.findById(teamReportId)
				.filter(t -> t.getProject().getWorkspace().getId().equals(principal.workspaceId()))
				.orElseThrow(TeamWeeklyReportNotFoundException::new);

		Project project = teamReport.getProject();
		return new TeamWeeklyReportExportView(buildTeamResponse(project, teamReport.getWeekStart()), project.getName());
	}

	// ----- FR-407: 조직 롤업 대시보드 -----

	/**
	 * GET /api/reports/rollup(계약 문서 fr407-contract.md). "팀"=Project(ACTIVE만), "조직"=요청자의
	 * 워크스페이스로 축소(멀티워크스페이스 임원 개념 없음). 완료율/지연율은 buildTeamResponse/countTeamIssues와
	 * 동일한 분모 원칙("완료+진행+이슈" 태스크 수)을 프로젝트(=팀) 단위로, computeAutoTaskSections의
	 * OVERDUE/STALE 판정 공식을 담당자 구분 없이 프로젝트 전체로 확장해 계산한다(새 계산식을 발명하지 않음).
	 */
	@Transactional(readOnly = true)
	public RollupResponse getOrgRollup(AuthenticatedUser principal, LocalDate weekStartParam) {
		LocalDate weekStart = resolveRollupWeekStart(weekStartParam);
		LocalDate weekEnd = weekEndOf(weekStart);
		Long workspaceId = principal.workspaceId();

		List<Project> activeProjects = projectRepository
				.findAllByWorkspaceIdAndStatusOrderByIdAsc(workspaceId, ProjectStatus.ACTIVE);
		int memberCount = reportMembers(workspaceId).size();

		List<RollupTeamItem> teams = new ArrayList<>();
		int orgCompleted = 0;
		int orgDenominator = 0;
		for (Project project : activeProjects) {
			ProjectTaskCounts counts = computeProjectTaskCounts(project.getId(), weekStart, weekEnd);
			int submittedCount = (int) weeklyReportRepository
					.countByProject_IdAndWeekStartAndStatus(project.getId(), weekStart, WeeklyReportStatus.SUBMITTED);
			int denominator = counts.denominator();
			teams.add(new RollupTeamItem(project.getId(), project.getName(), memberCount, submittedCount,
					percentOf(counts.completed(), denominator), percentOf(counts.overdueOnly(), denominator)));
			orgCompleted += counts.completed();
			orgDenominator += denominator;
		}
		int orgCompletionRate = percentOf(orgCompleted, orgDenominator);

		List<RollupTrendItem> trend = new ArrayList<>();
		for (int weeksAgo = 3; weeksAgo >= 1; weeksAgo--) {
			LocalDate trendWeekStart = weekStart.minusWeeks(weeksAgo);
			trend.add(computeOrgTrendItem(activeProjects, trendWeekStart));
		}
		trend.add(new RollupTrendItem(weekStart, weekEnd, orgCompletionRate));

		return new RollupResponse(weekStart, weekEnd, memberCount, teams, orgCompletionRate, trend);
	}

	private RollupTrendItem computeOrgTrendItem(List<Project> activeProjects, LocalDate trendWeekStart) {
		LocalDate trendWeekEnd = weekEndOf(trendWeekStart);
		int completed = 0;
		int denominator = 0;
		for (Project project : activeProjects) {
			ProjectTaskCounts counts = computeProjectTaskCounts(project.getId(), trendWeekStart, trendWeekEnd);
			completed += counts.completed();
			denominator += counts.denominator();
		}
		return new RollupTrendItem(trendWeekStart, trendWeekEnd, percentOf(completed, denominator));
	}

	/**
	 * FR-407 전용: computeAutoTaskSections와 동일한 OVERDUE/STALE 판정 공식(FR-401 계약 문서 그대로)을
	 * 담당자(assignees) 필터 없이 프로젝트 전체 단위로 확장한다. issuesTotal은 한 태스크가 OVERDUE·STALE을
	 * 동시에 만족하면 두 번 집계될 수 있다(computeAutoTaskSections의 이슈 목록과 동일한 특성을 그대로 유지).
	 */
	private ProjectTaskCounts computeProjectTaskCounts(Long projectId, LocalDate weekStart, LocalDate weekEnd) {
		LocalDateTime rangeStart = weekStart.atStartOfDay();
		LocalDateTime rangeEndExclusive = weekEnd.plusDays(1).atStartOfDay();

		int completed = taskRepository
				.findAllByProject_IdAndStatusAndUpdatedAtBetween(projectId, TaskStatus.DONE, rangeStart, rangeEndExclusive)
				.size();

		List<Task> openTasks = taskRepository.findAllByProject_IdAndStatusNot(projectId, TaskStatus.DONE);
		int inProgress = openTasks.size();

		LocalDate today = LocalDate.now(KST);
		LocalDate overdueCutoff = today.isBefore(weekEnd.plusDays(1)) ? today : weekEnd.plusDays(1);
		LocalDateTime staleThreshold = LocalDateTime.now(KST).minusDays(STALE_DAYS_THRESHOLD);

		int overdueOnly = 0;
		int issuesTotal = 0;
		for (Task t : openTasks) {
			if (t.getDueDate() != null && t.getDueDate().isBefore(overdueCutoff)) {
				overdueOnly++;
				issuesTotal++;
			}
			if (t.getUpdatedAt().isBefore(staleThreshold)) {
				issuesTotal++;
			}
		}
		return new ProjectTaskCounts(completed, inProgress, issuesTotal, overdueOnly);
	}

	private static int percentOf(int numerator, int denominator) {
		if (denominator == 0) {
			return 0;
		}
		return (int) Math.round((numerator * 100.0) / denominator);
	}

	/**
	 * 계약 문서 명시: weekStart 생략 시 이번 주(KST) 월요일을 기본값으로 쓰되, 값이 있는데 월요일이 아니면
	 * (resolveWeekStart처럼 조용히 정규화하지 않고) 400으로 거부한다. "임원 대시보드"는 필터 UI에서 항상
	 * 주 단위 옵션만 노출하므로, 월요일이 아닌 값이 들어오면 클라이언트 버그로 간주해 명시적으로 알린다.
	 */
	private LocalDate resolveRollupWeekStart(LocalDate weekStartParam) {
		if (weekStartParam == null) {
			return currentWeekStart();
		}
		if (weekStartParam.getDayOfWeek() != DayOfWeek.MONDAY) {
			throw new InvalidReportRequestException("weekStart는 반드시 월요일(yyyy-MM-dd)이어야 합니다.");
		}
		return weekStartParam;
	}

	private record ProjectTaskCounts(int completed, int inProgress, int issuesTotal, int overdueOnly) {
		int denominator() {
			return completed + inProgress + issuesTotal;
		}
	}

	// ----- 내부 구현 -----

	private WeeklyReport getOrCreateEntity(Project project, User user, LocalDate weekStart) {
		return weeklyReportRepository.findByProject_IdAndUser_IdAndWeekStart(project.getId(), user.getId(), weekStart)
				.orElseGet(() -> weeklyReportRepository
						.save(new WeeklyReport(project, user, weekStart, weekEndOf(weekStart))));
	}

	private WeeklyReportResponse toResponse(WeeklyReport report) {
		Long projectId = report.getProject().getId();
		LocalDate weekStart = report.getWeekStart();
		LocalDate weekEnd = report.getWeekEnd();

		AutoTaskSections sections = computeAutoTaskSections(projectId, report.getUser().getId(), weekStart, weekEnd);

		return new WeeklyReportResponse(
				report.getId(), projectId, weekStart, weekEnd, report.getStatus(), report.getNextWeekPlan(),
				report.getSubmittedAt(), report.getUpdatedAt(),
				sections.completed(), sections.inProgress(), sections.issues());
	}

	private TeamWeeklyReportResponse buildTeamResponse(Project project, LocalDate weekStart) {
		Long projectId = project.getId();
		LocalDate weekEnd = weekEndOf(weekStart);
		List<User> members = reportMembers(project.getWorkspace().getId());

		int submittedCount = 0;
		int teamCompletedCount = 0;
		int teamIssueCount = 0;
		List<TeamMemberReportSummary> memberSummaries = new ArrayList<>();

		for (User member : members) {
			Optional<WeeklyReport> maybeReport = weeklyReportRepository
					.findByProject_IdAndUser_IdAndWeekStart(projectId, member.getId(), weekStart);
			AutoTaskSections sections = computeAutoTaskSections(projectId, member.getId(), weekStart, weekEnd);

			boolean submitted = maybeReport.isPresent() && maybeReport.get().getStatus() == WeeklyReportStatus.SUBMITTED;
			if (submitted) {
				submittedCount++;
			}
			teamCompletedCount += sections.completed().size();
			teamIssueCount += sections.issues().size();

			memberSummaries.add(new TeamMemberReportSummary(
					member.getId(), member.getName(),
					submitted ? MemberSubmissionStatus.SUBMITTED : MemberSubmissionStatus.NOT_SUBMITTED,
					maybeReport.map(WeeklyReport::getSubmittedAt).orElse(null),
					sections.completed().size(), sections.inProgress().size(), sections.issues().size(),
					topTitles(sections.completed().stream().map(CompletedTaskItem::title).toList()),
					topTitles(sections.inProgress().stream().map(InProgressTaskItem::title).toList()),
					topTitles(sections.issues().stream().map(WeeklyReportService::issueTitleWithKind).toList()),
					maybeReport.map(WeeklyReport::getNextWeekPlan).orElse("")));
		}

		Optional<TeamWeeklyReport> teamReport = teamWeeklyReportRepository.findByProject_IdAndWeekStart(projectId, weekStart);

		return new TeamWeeklyReportResponse(
				projectId, weekStart, weekEnd,
				teamReport.map(TeamWeeklyReport::getPublishedAt).orElse(null),
				teamReport.map(t -> t.getPublishedBy().getName()).orElse(null),
				submittedCount, members.size(), teamCompletedCount, teamIssueCount, memberSummaries);
	}

	private int countTeamIssues(Project project, LocalDate weekStart, LocalDate weekEnd) {
		List<User> members = reportMembers(project.getWorkspace().getId());
		int total = 0;
		for (User member : members) {
			total += computeAutoTaskSections(project.getId(), member.getId(), weekStart, weekEnd).issues().size();
		}
		return total;
	}

	private boolean matchesKeyword(Long projectId, LocalDate weekStart, String keyword) {
		return weeklyReportRepository.existsNextWeekPlanMatch(projectId, weekStart, keyword);
	}

	/**
	 * FR-401 자동 취합 규칙(계약 문서 그대로):
	 * - 완료한 일: status=DONE AND assignees 포함 AND updatedAt in [weekStart, weekEnd+1일).
	 * - 진행 중인 일: status!=DONE AND assignees 포함. isNew=createdAt in [weekStart, weekEnd+1일).
	 * - OVERDUE: status!=DONE AND dueDate < min(오늘, weekEnd+1일) — 지난 주 보고서는 그 주 기준으로 판정.
	 * - STALE: status!=DONE AND updatedAt이 21일 이상 전(문면 그대로 "현재 시각" 기준. OVERDUE와 달리
	 *   과거 주차 조회 시에도 weekEnd로 재기준하지 않는다 — 계약 문서가 STALE에는 "그 주 기준" 문구를
	 *   두지 않은 것을 문면 그대로 해석함).
	 */
	private AutoTaskSections computeAutoTaskSections(Long projectId, Long userId, LocalDate weekStart, LocalDate weekEnd) {
		LocalDateTime rangeStart = weekStart.atStartOfDay();
		LocalDateTime rangeEndExclusive = weekEnd.plusDays(1).atStartOfDay();

		List<Task> completedTasks = taskRepository.findAllByProject_IdAndAssignees_IdAndStatusAndUpdatedAtBetween(
				projectId, userId, TaskStatus.DONE, rangeStart, rangeEndExclusive);
		List<CompletedTaskItem> completed = completedTasks.stream()
				.map(t -> new CompletedTaskItem(t.getId(), t.getTitle(), t.getDueDate(), t.getUpdatedAt()))
				.toList();

		List<Task> openTasks = taskRepository.findAllByProject_IdAndAssignees_IdAndStatusNot(
				projectId, userId, TaskStatus.DONE);
		List<InProgressTaskItem> inProgress = openTasks.stream()
				.map(t -> new InProgressTaskItem(t.getId(), t.getTitle(), t.getStatus(), t.getPriority(),
						t.getDueDate(), isInRange(t.getCreatedAt(), rangeStart, rangeEndExclusive)))
				.toList();

		LocalDate today = LocalDate.now(KST);
		LocalDate overdueCutoff = today.isBefore(weekEnd.plusDays(1)) ? today : weekEnd.plusDays(1);
		LocalDateTime staleThreshold = LocalDateTime.now(KST).minusDays(STALE_DAYS_THRESHOLD);

		List<IssueItem> issues = new ArrayList<>();
		for (Task t : openTasks) {
			if (t.getDueDate() != null && t.getDueDate().isBefore(overdueCutoff)) {
				long daysOverdue = ChronoUnit.DAYS.between(t.getDueDate(), overdueCutoff);
				issues.add(new IssueItem(t.getId(), t.getTitle(), IssueKind.OVERDUE, t.getDueDate(), daysOverdue, null));
			}
			if (t.getUpdatedAt().isBefore(staleThreshold)) {
				issues.add(new IssueItem(t.getId(), t.getTitle(), IssueKind.STALE, null, null,
						t.getUpdatedAt().toLocalDate()));
			}
		}

		return new AutoTaskSections(completed, inProgress, issues);
	}

	private static boolean isInRange(LocalDateTime value, LocalDateTime startInclusive, LocalDateTime endExclusive) {
		return !value.isBefore(startInclusive) && value.isBefore(endExclusive);
	}

	private static List<String> topTitles(List<String> titles) {
		return titles.stream().limit(TOP_TITLES_LIMIT).toList();
	}

	/**
	 * 팀 보고서 요약의 이슈 제목에는 종류(마감초과/정체) 라벨을 접미사로 붙인다. 개인 보고서 화면은
	 * IssueItem.kind로 배지를 구분 렌더링하지만, 팀 요약은 제목 문자열 배열만 내려주므로 한 태스크가
	 * OVERDUE·STALE을 동시에 만족해 같은 제목이 두 번 들어갈 때 구분이 사라지는 문제가 있었다(QA 결함 1).
	 */
	private static String issueTitleWithKind(IssueItem issue) {
		String label = issue.kind() == IssueKind.OVERDUE ? "마감초과" : "정체";
		return issue.title() + " (" + label + ")";
	}

	/**
	 * 클라이언트가 보낸 weekStart를 그대로 신뢰하지 않고 항상 그 주의 월요일로 정규화한다(방어적 정규화 —
	 * 계약 문서는 "서버는 임의의 weekStart를 받으므로 클라이언트에서 월요일 기준으로 계산" 이라고만 명시하나,
	 * 잘못 정렬된 날짜가 들어와도 project_id+user_id+week_start 유니크 제약과 주차 경계가 어긋나지 않도록
	 * 서버에서도 한번 더 보정한다). 값이 없으면 Asia/Seoul 기준 이번 주 월요일을 기본값으로 쓴다.
	 */
	private LocalDate resolveWeekStart(LocalDate weekStartParam) {
		LocalDate base = weekStartParam != null ? weekStartParam : LocalDate.now(KST);
		return base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	private LocalDate currentWeekStart() {
		return LocalDate.now(KST).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	private static LocalDate weekEndOf(LocalDate weekStart) {
		return weekStart.plusDays(6);
	}

	// FR-004/FR-410과 동일 원칙: LIKE 와일드카드(%, _) 이스케이프.
	private static String escapeLikeWildcards(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private Project getProjectInWorkspace(AuthenticatedUser principal, Long projectId) {
		return projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
	}

	/**
	 * 주간 보고 대상 "멤버"는 GUEST를 제외한 워크스페이스 사용자다. GUEST는 개인 보고서 엔드포인트
	 * (GET /reports/me 등)가 구조적으로 403이라 애초에 보고서를 제출할 수 없으므로, 제출률 분모나
	 * 미제출 리마인드 대상에 포함하면 (1) 제출률이 영원히 100%에 못 미치고 (2) GUEST에게 의미 없는
	 * 리마인드 알림이 매주 발송되는 문제가 생긴다. 계약 문서는 "프로젝트 멤버 전원"을 문자 그대로
	 * 재사용하라고 했으나, 그 전제(멤버=제출 가능자)가 GUEST에는 성립하지 않아 여기서만 GUEST를 뺀다.
	 */
	private List<User> reportMembers(Long workspaceId) {
		return userRepository.findAllByWorkspaceIdOrderByNameAsc(workspaceId).stream()
				.filter(u -> u.getRole() != Role.GUEST)
				.toList();
	}

	private record AutoTaskSections(
			List<CompletedTaskItem> completed,
			List<InProgressTaskItem> inProgress,
			List<IssueItem> issues) {
	}
}
