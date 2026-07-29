package com.teamsync.back.report;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.InvalidReportRequestException;
import com.teamsync.back.common.exception.WeeklyReportAlreadySubmittedException;
import com.teamsync.back.common.exception.WeeklyReportNotFoundException;
import com.teamsync.back.report.dto.EntriesReplaceRequest;
import com.teamsync.back.report.dto.EntryResponse;
import com.teamsync.back.report.dto.EntryUpsertRequest;
import com.teamsync.back.report.dto.ExecutiveCategoryGroup;
import com.teamsync.back.report.dto.ExecutiveDashboardResponse;
import com.teamsync.back.report.dto.ExecutiveMemberEntries;
import com.teamsync.back.report.dto.MemberSubmissionStatus;
import com.teamsync.back.report.dto.ReportEntries;
import com.teamsync.back.report.dto.TeamDashboardResponse;
import com.teamsync.back.report.dto.TeamMemberReportEntries;
import com.teamsync.back.report.dto.TeamWeeklyReportExportView;
import com.teamsync.back.report.dto.WeeklyReportExportView;
import com.teamsync.back.report.dto.WeeklyReportResponse;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주간 보고(V23 재설계) 서비스. 기존 Task 자동 취합(computeAutoTaskSections)은 완전히 폐기하고,
 * 사용자가 직접 입력한 {@link WeeklyReportEntry}(대/중/소분류 + 상세업무 + 달성율) 행을 그대로
 * 조회/치환한다. WeeklyReport는 project에 종속되지 않고 user_id + week_start 단위로 하나만
 * 존재한다(한 사람이 한 주에 여러 프로젝트 업무를 섞어 적을 수 있어야 하므로). 팀장/대표 뷰는
 * "발행(publish)" 개념 없이 매 요청마다 실시간으로 집계한다.
 */
@Service
public class WeeklyReportService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final WeeklyReportRepository weeklyReportRepository;
	private final WeeklyReportEntryRepository weeklyReportEntryRepository;
	private final CategoryKeywordRepository categoryKeywordRepository;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;

	public WeeklyReportService(WeeklyReportRepository weeklyReportRepository,
			WeeklyReportEntryRepository weeklyReportEntryRepository,
			CategoryKeywordRepository categoryKeywordRepository, UserRepository userRepository,
			ProjectRepository projectRepository) {
		this.weeklyReportRepository = weeklyReportRepository;
		this.weeklyReportEntryRepository = weeklyReportEntryRepository;
		this.categoryKeywordRepository = categoryKeywordRepository;
		this.userRepository = userRepository;
		this.projectRepository = projectRepository;
	}

	// ----- 개인 보고서 -----

	@Transactional
	public WeeklyReportResponse getOrCreateMyReport(AuthenticatedUser principal, LocalDate weekStartParam) {
		LocalDate weekStart = resolveWeekStart(weekStartParam);
		User user = userRepository.getReferenceById(principal.userId());
		WeeklyReport report = getOrCreateEntity(user, weekStart);
		return toResponse(report);
	}

	/**
	 * 해당 section(THIS_WEEK/NEXT_WEEK)의 기존 행을 통째로 지우고 요청 리스트로 재생성한다(행 추가/삭제/
	 * 순서변경을 프론트가 한 번에 PUT). orderIndex는 요청 리스트 순서를 그대로 반영한다. SUBMITTED
	 * 상태에서는 "확정 이후 원본 불변" 원칙에 따라 거부한다.
	 */
	@Transactional
	public WeeklyReportResponse replaceEntries(AuthenticatedUser principal, LocalDate weekStartParam,
			EntrySection section, EntriesReplaceRequest request) {
		LocalDate weekStart = resolveWeekStart(weekStartParam);
		User user = userRepository.getReferenceById(principal.userId());
		WeeklyReport report = getOrCreateEntity(user, weekStart);
		if (report.getStatus() == WeeklyReportStatus.SUBMITTED) {
			throw new WeeklyReportAlreadySubmittedException();
		}

		List<WeeklyReportEntry> newEntries = new ArrayList<>();
		int orderIndex = 0;
		for (EntryUpsertRequest entryRequest : request.entries()) {
			Project project = getProjectInWorkspace(entryRequest.projectId(), principal.workspaceId());
			CategoryKeyword middle = getActiveCategory(entryRequest.middleCategoryId(), CategoryType.MIDDLE);
			newEntries.add(new WeeklyReportEntry(report, section, project, middle, entryRequest.minorCategory(),
					entryRequest.detail(), entryRequest.ratePercent(), orderIndex++));
		}

		weeklyReportEntryRepository.deleteAllByReport_IdAndSection(report.getId(), section);
		weeklyReportEntryRepository.saveAll(newEntries);

		return toResponse(report);
	}

	@Transactional
	public WeeklyReportResponse submitMyReport(AuthenticatedUser principal, LocalDate weekStartParam) {
		LocalDate weekStart = resolveWeekStart(weekStartParam);
		User user = userRepository.getReferenceById(principal.userId());
		WeeklyReport report = getOrCreateEntity(user, weekStart);
		if (report.getStatus() == WeeklyReportStatus.SUBMITTED) {
			throw new WeeklyReportAlreadySubmittedException();
		}
		report.submit();
		return toResponse(report);
	}

	/**
	 * "다시 작성하기": 이미 제출한 보고서를 실수로 잘못 냈을 때 사용자가 직접 SUBMITTED -> DRAFT로
	 * 되돌려 행 추가/수정 후 재제출할 수 있게 한다. 이미 DRAFT면 그대로 두고(idempotent) 응답만 반환한다.
	 */
	@Transactional
	public WeeklyReportResponse reopenMyReport(AuthenticatedUser principal, LocalDate weekStartParam) {
		LocalDate weekStart = resolveWeekStart(weekStartParam);
		User user = userRepository.getReferenceById(principal.userId());
		WeeklyReport report = getOrCreateEntity(user, weekStart);
		if (report.getStatus() == WeeklyReportStatus.SUBMITTED) {
			report.reopen();
		}
		return toResponse(report);
	}

	// ----- 팀장 뷰 -----

	@Transactional(readOnly = true)
	public TeamDashboardResponse getTeamDashboard(AuthenticatedUser principal, LocalDate weekStartParam) {
		LocalDate weekStart = resolveWeekStart(weekStartParam);
		LocalDate weekEnd = weekEndOf(weekStart);
		List<User> members = reportMembers(principal.workspaceId());
		List<Long> userIds = members.stream().map(User::getId).toList();

		List<WeeklyReport> reports = weeklyReportRepository.findAllByUser_IdInAndWeekStart(userIds, weekStart);
		Map<Long, WeeklyReport> reportByUserId = reports.stream()
				.collect(Collectors.toMap(r -> r.getUser().getId(), r -> r));
		Map<Long, List<WeeklyReportEntry>> entriesByReportId = loadEntriesByReportId(reports);

		int submittedCount = 0;
		List<TeamMemberReportEntries> memberList = new ArrayList<>();
		for (User member : members) {
			WeeklyReport report = reportByUserId.get(member.getId());
			boolean submitted = report != null && report.getStatus() == WeeklyReportStatus.SUBMITTED;
			if (submitted) {
				submittedCount++;
			}
			List<WeeklyReportEntry> memberEntries = report != null
					? entriesByReportId.getOrDefault(report.getId(), List.of())
					: List.of();
			memberList.add(new TeamMemberReportEntries(member.getId(), member.getName(),
					submitted ? MemberSubmissionStatus.SUBMITTED : MemberSubmissionStatus.NOT_SUBMITTED,
					report != null ? report.getSubmittedAt() : null,
					filterSection(memberEntries, EntrySection.THIS_WEEK),
					filterSection(memberEntries, EntrySection.NEXT_WEEK)));
		}

		return new TeamDashboardResponse(weekStart, weekEnd, submittedCount, members.size(), memberList);
	}

	// ----- 대표 뷰 -----

	/**
	 * 전 인원의 entries를 project(대분류) 기준으로 그룹핑한다. 해당 프로젝트에 entry가 있는 멤버만
	 * 포함하고(entry 없는 조합은 생략), 그룹 안 각 멤버의 thisWeekEntries/nextWeekEntries는 그
	 * 프로젝트에 속한 항목만 필터링된 목록이다(멤버 전체 항목이 아님).
	 */
	@Transactional(readOnly = true)
	public ExecutiveDashboardResponse getExecutiveDashboard(AuthenticatedUser principal, LocalDate weekStartParam) {
		LocalDate weekStart = resolveWeekStart(weekStartParam);
		LocalDate weekEnd = weekEndOf(weekStart);
		List<User> members = reportMembers(principal.workspaceId());
		List<Long> userIds = members.stream().map(User::getId).toList();

		List<WeeklyReport> reports = weeklyReportRepository.findAllByUser_IdInAndWeekStart(userIds, weekStart);
		Map<Long, Long> reportIdToUserId = reports.stream()
				.collect(Collectors.toMap(WeeklyReport::getId, r -> r.getUser().getId()));
		List<WeeklyReportEntry> entries = loadEntries(reports);

		Map<Long, Project> projectById = new LinkedHashMap<>();
		Map<Long, Map<Long, List<EntryResponse>>> thisWeekByCategoryThenUser = new LinkedHashMap<>();
		Map<Long, Map<Long, List<EntryResponse>>> nextWeekByCategoryThenUser = new LinkedHashMap<>();

		for (WeeklyReportEntry entry : entries) {
			Long userId = reportIdToUserId.get(entry.getReport().getId());
			if (userId == null) {
				continue;
			}
			Project project = entry.getProject();
			projectById.putIfAbsent(project.getId(), project);
			Map<Long, Map<Long, List<EntryResponse>>> target = entry.getSection() == EntrySection.THIS_WEEK
					? thisWeekByCategoryThenUser
					: nextWeekByCategoryThenUser;
			target.computeIfAbsent(project.getId(), key -> new LinkedHashMap<>())
					.computeIfAbsent(userId, key -> new ArrayList<>())
					.add(EntryResponse.from(entry));
		}

		List<ExecutiveCategoryGroup> categories = projectById.values().stream()
				.sorted(Comparator.comparing(Project::getName))
				.map(project -> buildCategoryGroup(project, members, thisWeekByCategoryThenUser, nextWeekByCategoryThenUser))
				.toList();

		return new ExecutiveDashboardResponse(weekStart, weekEnd, categories);
	}

	private ExecutiveCategoryGroup buildCategoryGroup(Project project, List<User> members,
			Map<Long, Map<Long, List<EntryResponse>>> thisWeekByCategoryThenUser,
			Map<Long, Map<Long, List<EntryResponse>>> nextWeekByCategoryThenUser) {
		Map<Long, List<EntryResponse>> thisWeekByUser = thisWeekByCategoryThenUser.getOrDefault(project.getId(), Map.of());
		Map<Long, List<EntryResponse>> nextWeekByUser = nextWeekByCategoryThenUser.getOrDefault(project.getId(), Map.of());
		Set<Long> memberIdsInCategory = new LinkedHashSet<>();
		memberIdsInCategory.addAll(thisWeekByUser.keySet());
		memberIdsInCategory.addAll(nextWeekByUser.keySet());

		List<ExecutiveMemberEntries> memberEntries = members.stream()
				.filter(member -> memberIdsInCategory.contains(member.getId()))
				.map(member -> new ExecutiveMemberEntries(member.getId(), member.getName(),
						thisWeekByUser.getOrDefault(member.getId(), List.of()),
						nextWeekByUser.getOrDefault(member.getId(), List.of())))
				.toList();

		return new ExecutiveCategoryGroup(project.getId(), project.getName(), memberEntries);
	}

	// ----- FR-409: 보고서 내보내기(PDF/이메일/xlsx) -----

	/**
	 * reportId로 개인 주간 보고서를 직접 조회한다. "본인 또는 같은 워크스페이스 LEADER/ADMIN만 허용"
	 * (그 외엔 403이 아닌 404로 응답해 리소스 존재 여부를 숨긴다, PRD 5.6).
	 */
	@Transactional(readOnly = true)
	public WeeklyReportExportView getReportForExport(AuthenticatedUser principal, Long reportId) {
		WeeklyReport report = weeklyReportRepository.findById(reportId)
				.filter(r -> r.getUser().getWorkspace().getId().equals(principal.workspaceId()))
				.orElseThrow(WeeklyReportNotFoundException::new);

		boolean isOwner = report.getUser().getId().equals(principal.userId());
		boolean isLeaderOrAdmin = principal.role() == Role.ADMIN || principal.role() == Role.LEADER;
		if (!isOwner && !isLeaderOrAdmin) {
			throw new WeeklyReportNotFoundException();
		}

		return new WeeklyReportExportView(toResponse(report), report.getUser().getName(), report.getUser().getEmail());
	}

	/** 팀 보고서는 발행 레코드가 없으므로 teamReportId 대신 weekStart로 식별한다(LEADER/ADMIN, 컨트롤러에서 제한). */
	@Transactional(readOnly = true)
	public TeamWeeklyReportExportView getTeamReportForExport(AuthenticatedUser principal, LocalDate weekStart) {
		return new TeamWeeklyReportExportView(getTeamDashboard(principal, weekStart));
	}

	// ----- 내부 구현 -----

	private WeeklyReport getOrCreateEntity(User user, LocalDate weekStart) {
		return weeklyReportRepository.findByUser_IdAndWeekStart(user.getId(), weekStart)
				.orElseGet(() -> weeklyReportRepository.save(new WeeklyReport(user, weekStart, weekEndOf(weekStart))));
	}

	private Project getProjectInWorkspace(Long projectId, Long workspaceId) {
		return projectRepository.findByIdAndWorkspaceId(projectId, workspaceId)
				.orElseThrow(() -> new InvalidReportRequestException(
						"존재하지 않거나 워크스페이스에 속하지 않는 프로젝트입니다: " + projectId));
	}

	private CategoryKeyword getActiveCategory(Long id, CategoryType expectedType) {
		CategoryKeyword category = categoryKeywordRepository.findById(id)
				.filter(CategoryKeyword::isActive)
				.orElseThrow(() -> new InvalidReportRequestException("존재하지 않거나 비활성화된 카테고리입니다: " + id));
		if (category.getType() != expectedType) {
			throw new InvalidReportRequestException("카테고리 타입이 일치하지 않습니다(" + expectedType + " 기대, " + category.getType()
					+ " 입력): " + id);
		}
		return category;
	}

	private WeeklyReportResponse toResponse(WeeklyReport report) {
		List<WeeklyReportEntry> entries = weeklyReportEntryRepository
				.findAllByReport_IdOrderBySectionAscOrderIndexAsc(report.getId());
		ReportEntries reportEntries = new ReportEntries(
				filterSection(entries, EntrySection.THIS_WEEK), filterSection(entries, EntrySection.NEXT_WEEK));
		return new WeeklyReportResponse(report.getId(), report.getWeekStart(), report.getWeekEnd(), report.getStatus(),
				report.getSubmittedAt(), report.getUpdatedAt(), reportEntries);
	}

	private static List<EntryResponse> filterSection(List<WeeklyReportEntry> entries, EntrySection section) {
		return entries.stream().filter(e -> e.getSection() == section).map(EntryResponse::from).toList();
	}

	private Map<Long, List<WeeklyReportEntry>> loadEntriesByReportId(List<WeeklyReport> reports) {
		List<WeeklyReportEntry> entries = loadEntries(reports);
		return entries.stream().collect(
				Collectors.groupingBy(e -> e.getReport().getId(), LinkedHashMap::new, Collectors.toList()));
	}

	private List<WeeklyReportEntry> loadEntries(List<WeeklyReport> reports) {
		if (reports.isEmpty()) {
			return List.of();
		}
		List<Long> reportIds = reports.stream().map(WeeklyReport::getId).toList();
		return weeklyReportEntryRepository.findAllByReport_IdInOrderBySectionAscOrderIndexAsc(reportIds);
	}

	/**
	 * 클라이언트가 보낸 weekStart를 그대로 신뢰하지 않고 항상 그 주의 월요일로 정규화한다(방어적
	 * 정규화). 값이 없으면 Asia/Seoul 기준 이번 주 월요일을 기본값으로 쓴다.
	 */
	private LocalDate resolveWeekStart(LocalDate weekStartParam) {
		LocalDate base = weekStartParam != null ? weekStartParam : LocalDate.now(KST);
		return base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	private static LocalDate weekEndOf(LocalDate weekStart) {
		return weekStart.plusDays(6);
	}

	/**
	 * 주간 보고 대상 "멤버"는 GUEST를 제외한 워크스페이스 사용자다. GUEST는 개인 보고서 엔드포인트
	 * (GET /reports/me 등)가 구조적으로 403이라 애초에 보고서를 제출할 수 없으므로, 제출률 분모에
	 * 포함하면 제출률이 영원히 100%에 못 미치는 문제가 생긴다.
	 */
	private List<User> reportMembers(Long workspaceId) {
		return userRepository.findAllByWorkspaceIdOrderByNameAsc(workspaceId).stream()
				.filter(u -> u.getRole() != Role.GUEST)
				.toList();
	}
}
