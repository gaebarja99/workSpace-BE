package com.teamsync.back.report;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.channel.message.Message;
import com.teamsync.back.channel.message.MessageRepository;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.common.exception.WeeklyReportAlreadySubmittedException;
import com.teamsync.back.notification.NotificationService;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.report.dto.CompletedTaskItem;
import com.teamsync.back.report.dto.HighlightItem;
import com.teamsync.back.report.dto.InProgressTaskItem;
import com.teamsync.back.report.dto.IssueItem;
import com.teamsync.back.report.dto.IssueKind;
import com.teamsync.back.report.dto.MemberSubmissionStatus;
import com.teamsync.back.report.dto.NextWeekPlanUpdateRequest;
import com.teamsync.back.report.dto.RemindResponse;
import com.teamsync.back.report.dto.ReportHistoryItem;
import com.teamsync.back.report.dto.ReportHistoryStatus;
import com.teamsync.back.report.dto.TeamMemberReportSummary;
import com.teamsync.back.report.dto.TeamWeeklyReportResponse;
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
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-401~404/408/410(P2 주간 보고 자동화). 계약 문서(p2-weekly-report-contract.md) 기준 원칙:
 * 완료/진행/하이라이트/이슈 섹션은 WeeklyReport/TeamWeeklyReport 어디에도 스냅샷하지 않고 매 요청마다
 * project(+user)+weekStart~weekEnd 범위로 실시간 계산한다. FR-405/406/407/409(P3)는 이번 스코프 밖이며,
 * 계약 문서에 명시된 "3주 이상 정체(STALE)" 플래그만 FR-401 이슈 섹션의 축소판으로 포함한다.
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
	private final MessageRepository messageRepository;
	private final NotificationService notificationService;

	public WeeklyReportService(WeeklyReportRepository weeklyReportRepository,
			TeamWeeklyReportRepository teamWeeklyReportRepository, ProjectRepository projectRepository,
			UserRepository userRepository, TaskRepository taskRepository, MessageRepository messageRepository,
			NotificationService notificationService) {
		this.weeklyReportRepository = weeklyReportRepository;
		this.teamWeeklyReportRepository = teamWeeklyReportRepository;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
		this.taskRepository = taskRepository;
		this.messageRepository = messageRepository;
		this.notificationService = notificationService;
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

	// ----- FR-404/408: 팀 보고서 -----

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

	@Transactional
	public RemindResponse remindUnsubmitted(AuthenticatedUser principal, Long projectId, LocalDate weekStartParam) {
		Project project = getProjectInWorkspace(principal, projectId);
		LocalDate weekStart = resolveWeekStart(weekStartParam);

		List<User> unsubmitted = findUnsubmittedMembers(principal.workspaceId(), projectId, weekStart);
		// FR-408 수동 재발송: 멱등성 체크 없이 매번 즉시 발송(계약 문서 명시).
		for (User member : unsubmitted) {
			notificationService.notifyWeeklyReportReminder(project, member);
		}
		return new RemindResponse(unsubmitted.size());
	}

	/**
	 * FR-408 자동 배치: 매주 금요일 09:00 KST, 시스템 전역(모든 워크스페이스/프로젝트) 대상. 같은
	 * 주/같은 타입(WEEKLY_REPORT_REMINDER)으로 이미 발송된 수신자는 스킵한다(NotificationService의
	 * 멱등성 검사, 계약 문서: "제출 마감 시간 설정" UI는 이번 스코프 제외 — 금요일 18:00 KST 고정값 문서화만).
	 * 존재하지 않는 프로젝트가 대량으로 있어도 이 배치는 프로젝트 수 x 워크스페이스 인원 수준의 트래픽이라
	 * FR-108 마감 임박 배치(전 태스크 순회)와 동일한 수준의 비용으로 간주해 별도 페이징 없이 처리한다.
	 */
	@Scheduled(cron = "0 0 9 * * FRI", zone = "Asia/Seoul")
	@Transactional
	public void remindUnsubmittedReports() {
		LocalDate weekStart = currentWeekStart();
		LocalDate weekEnd = weekEndOf(weekStart);
		LocalDateTime rangeStart = weekStart.atStartOfDay();
		LocalDateTime rangeEndExclusive = weekEnd.plusDays(1).atStartOfDay();

		for (Project project : projectRepository.findAll()) {
			List<User> unsubmitted = findUnsubmittedMembers(project.getWorkspace().getId(), project.getId(), weekStart);
			for (User member : unsubmitted) {
				notificationService.notifyWeeklyReportReminderIfNeeded(project, member, rangeStart, rangeEndExclusive);
			}
		}
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
			if (keyword != null && !matchesKeyword(projectId, weekStart, weekEnd, keyword)) {
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
		List<HighlightItem> highlights = computeHighlights(projectId, weekStart, weekEnd);

		return new WeeklyReportResponse(
				report.getId(), projectId, weekStart, weekEnd, report.getStatus(), report.getNextWeekPlan(),
				report.getSubmittedAt(), report.getUpdatedAt(),
				sections.completed(), sections.inProgress(), highlights, sections.issues());
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

	private List<User> findUnsubmittedMembers(Long workspaceId, Long projectId, LocalDate weekStart) {
		List<User> members = reportMembers(workspaceId);
		Set<Long> submittedUserIds = weeklyReportRepository.findAllByProject_IdAndWeekStart(projectId, weekStart).stream()
				.filter(r -> r.getStatus() == WeeklyReportStatus.SUBMITTED)
				.map(r -> r.getUser().getId())
				.collect(Collectors.toSet());
		return members.stream().filter(m -> !submittedUserIds.contains(m.getId())).toList();
	}

	private int countTeamIssues(Project project, LocalDate weekStart, LocalDate weekEnd) {
		List<User> members = reportMembers(project.getWorkspace().getId());
		int total = 0;
		for (User member : members) {
			total += computeAutoTaskSections(project.getId(), member.getId(), weekStart, weekEnd).issues().size();
		}
		return total;
	}

	private boolean matchesKeyword(Long projectId, LocalDate weekStart, LocalDate weekEnd, String keyword) {
		if (weeklyReportRepository.existsNextWeekPlanMatch(projectId, weekStart, keyword)) {
			return true;
		}
		LocalDateTime start = weekStart.atStartOfDay();
		LocalDateTime end = weekEnd.plusDays(1).atStartOfDay();
		return messageRepository.existsHighlightedContentMatch(projectId, start, end, keyword);
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

	private List<HighlightItem> computeHighlights(Long projectId, LocalDate weekStart, LocalDate weekEnd) {
		LocalDateTime rangeStart = weekStart.atStartOfDay();
		LocalDateTime rangeEndExclusive = weekEnd.plusDays(1).atStartOfDay();
		return messageRepository.findHighlightsForProjectAndWeek(projectId, rangeStart, rangeEndExclusive).stream()
				.map(this::toHighlightItem)
				.toList();
	}

	private HighlightItem toHighlightItem(Message message) {
		String authorName = message.getAuthor() != null ? message.getAuthor().getName() : "시스템";
		return new HighlightItem(message.getId(), message.getChannel().getId(), message.getChannel().getName(),
				authorName, message.getContent(), message.getCreatedAt());
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
