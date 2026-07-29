package com.teamsync.back.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.InvalidReportRequestException;
import com.teamsync.back.common.exception.WeeklyReportAlreadySubmittedException;
import com.teamsync.back.common.exception.WeeklyReportNotFoundException;
import com.teamsync.back.report.dto.EntriesReplaceRequest;
import com.teamsync.back.report.dto.EntryUpsertRequest;
import com.teamsync.back.report.dto.ExecutiveDashboardResponse;
import com.teamsync.back.report.dto.TeamDashboardResponse;
import com.teamsync.back.report.dto.TeamWeeklyReportExportView;
import com.teamsync.back.report.dto.WeeklyReportExportView;
import com.teamsync.back.report.dto.WeeklyReportResponse;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.workspace.Workspace;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 주간 보고(V23 재설계) 핵심 로직 단위 테스트: 대/중/소분류 + 달성율 행 치환, 제출 후 불변, 팀/대표 뷰
 * 집계, 내보내기 권한 검증.
 */
@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceTest {

	@Mock
	private WeeklyReportRepository weeklyReportRepository;

	@Mock
	private WeeklyReportEntryRepository weeklyReportEntryRepository;

	@Mock
	private CategoryKeywordRepository categoryKeywordRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ProjectRepository projectRepository;

	private WeeklyReportService weeklyReportService;
	private Workspace workspace;
	private AuthenticatedUser memberPrincipal;

	private static final LocalDate WEEK_START = LocalDate.of(2020, 1, 6); // 고정 과거 월요일
	private static final LocalDate WEEK_END = WEEK_START.plusDays(6);

	@BeforeEach
	void setUp() throws Exception {
		weeklyReportService = new WeeklyReportService(weeklyReportRepository, weeklyReportEntryRepository,
				categoryKeywordRepository, userRepository, projectRepository);
		workspace = new Workspace("그로우테크", "growtech.io");
		setId(workspace, 10L);
		memberPrincipal = new AuthenticatedUser(5L, 10L, "member@growtech.io", Role.STAFF);
	}

	@Test
	void replaceEntries는_중분류_타입이_바뀌면_예외() throws Exception {
		User user = newUser("멤버", Role.STAFF);
		setId(user, 5L);
		WeeklyReport report = new WeeklyReport(user, WEEK_START, WEEK_END);
		setId(report, 900L);
		when(userRepository.getReferenceById(5L)).thenReturn(user);
		when(weeklyReportRepository.findByUser_IdAndWeekStart(5L, WEEK_START)).thenReturn(Optional.of(report));

		Project project = newProject("rCMS (당원관리)");
		setId(project, 1L);
		when(projectRepository.findByIdAndWorkspaceId(1L, 10L)).thenReturn(Optional.of(project));

		CategoryKeyword majorTypeAsMiddle = newCategory(CategoryType.MAJOR, "rCMS (당원관리)");
		setId(majorTypeAsMiddle, 2L);
		when(categoryKeywordRepository.findById(2L)).thenReturn(Optional.of(majorTypeAsMiddle));

		EntriesReplaceRequest request = new EntriesReplaceRequest(
				List.of(new EntryUpsertRequest(1L, 2L, "소분류", "상세", 50)));

		assertThatThrownBy(() -> weeklyReportService.replaceEntries(memberPrincipal, WEEK_START,
				EntrySection.THIS_WEEK, request))
				.isInstanceOf(InvalidReportRequestException.class);
	}

	@Test
	void replaceEntries는_워크스페이스에_속하지_않는_프로젝트면_예외() throws Exception {
		User user = newUser("멤버", Role.STAFF);
		setId(user, 5L);
		WeeklyReport report = new WeeklyReport(user, WEEK_START, WEEK_END);
		setId(report, 900L);
		when(userRepository.getReferenceById(5L)).thenReturn(user);
		when(weeklyReportRepository.findByUser_IdAndWeekStart(5L, WEEK_START)).thenReturn(Optional.of(report));
		when(projectRepository.findByIdAndWorkspaceId(1L, 10L)).thenReturn(Optional.empty());

		EntriesReplaceRequest request = new EntriesReplaceRequest(
				List.of(new EntryUpsertRequest(1L, 2L, "소분류", "상세", 50)));

		assertThatThrownBy(() -> weeklyReportService.replaceEntries(memberPrincipal, WEEK_START,
				EntrySection.THIS_WEEK, request))
				.isInstanceOf(InvalidReportRequestException.class);
	}

	@Test
	void replaceEntries는_정상_요청이면_기존_행을_지우고_새로_생성한다() throws Exception {
		User user = newUser("멤버", Role.STAFF);
		setId(user, 5L);
		WeeklyReport report = new WeeklyReport(user, WEEK_START, WEEK_END);
		setId(report, 900L);
		when(userRepository.getReferenceById(5L)).thenReturn(user);
		when(weeklyReportRepository.findByUser_IdAndWeekStart(5L, WEEK_START)).thenReturn(Optional.of(report));

		Project project = newProject("rCMS (당원관리)");
		setId(project, 1L);
		CategoryKeyword middle = newCategory(CategoryType.MIDDLE, "개발/구현");
		setId(middle, 2L);
		when(projectRepository.findByIdAndWorkspaceId(1L, 10L)).thenReturn(Optional.of(project));
		when(categoryKeywordRepository.findById(2L)).thenReturn(Optional.of(middle));
		when(weeklyReportEntryRepository.findAllByReport_IdOrderBySectionAscOrderIndexAsc(900L)).thenReturn(List.of());

		EntriesReplaceRequest request = new EntriesReplaceRequest(
				List.of(new EntryUpsertRequest(1L, 2L, "소분류", "상세업무", 70)));

		WeeklyReportResponse response = weeklyReportService.replaceEntries(memberPrincipal, WEEK_START,
				EntrySection.THIS_WEEK, request);

		assertThat(response.id()).isEqualTo(900L);
		org.mockito.Mockito.verify(weeklyReportEntryRepository)
				.deleteAllByReport_IdAndSection(900L, EntrySection.THIS_WEEK);
		org.mockito.Mockito.verify(weeklyReportEntryRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
	}

	@Test
	void 이미_제출된_보고서는_수정_불가() throws Exception {
		User user = newUser("멤버", Role.STAFF);
		setId(user, 5L);
		WeeklyReport report = new WeeklyReport(user, WEEK_START, WEEK_END);
		report.submit();
		setId(report, 900L);
		when(userRepository.getReferenceById(5L)).thenReturn(user);
		when(weeklyReportRepository.findByUser_IdAndWeekStart(5L, WEEK_START)).thenReturn(Optional.of(report));

		EntriesReplaceRequest request = new EntriesReplaceRequest(List.of());

		assertThatThrownBy(() -> weeklyReportService.replaceEntries(memberPrincipal, WEEK_START,
				EntrySection.THIS_WEEK, request))
				.isInstanceOf(WeeklyReportAlreadySubmittedException.class);
	}

	@Test
	void 이미_제출된_보고서를_다시_제출하면_예외() throws Exception {
		User user = newUser("멤버", Role.STAFF);
		setId(user, 5L);
		WeeklyReport report = new WeeklyReport(user, WEEK_START, WEEK_END);
		report.submit();
		when(userRepository.getReferenceById(5L)).thenReturn(user);
		when(weeklyReportRepository.findByUser_IdAndWeekStart(5L, WEEK_START)).thenReturn(Optional.of(report));

		assertThatThrownBy(() -> weeklyReportService.submitMyReport(memberPrincipal, WEEK_START))
				.isInstanceOf(WeeklyReportAlreadySubmittedException.class);
	}

	@Test
	void 제출된_보고서를_다시_작성하면_DRAFT로_되돌아간다() throws Exception {
		User user = newUser("멤버", Role.STAFF);
		setId(user, 5L);
		WeeklyReport report = new WeeklyReport(user, WEEK_START, WEEK_END);
		report.submit();
		setId(report, 900L);
		when(userRepository.getReferenceById(5L)).thenReturn(user);
		when(weeklyReportRepository.findByUser_IdAndWeekStart(5L, WEEK_START)).thenReturn(Optional.of(report));
		when(weeklyReportEntryRepository.findAllByReport_IdOrderBySectionAscOrderIndexAsc(900L)).thenReturn(List.of());

		WeeklyReportResponse response = weeklyReportService.reopenMyReport(memberPrincipal, WEEK_START);

		assertThat(response.status()).isEqualTo(WeeklyReportStatus.DRAFT);
		assertThat(response.submittedAt()).isNull();

		// 다시 작성 후에는 entries PUT/제출이 다시 허용되어야 한다.
		Project project = newProject("rCMS (당원관리)");
		setId(project, 1L);
		CategoryKeyword middle = newCategory(CategoryType.MIDDLE, "개발/구현");
		setId(middle, 2L);
		when(projectRepository.findByIdAndWorkspaceId(1L, 10L)).thenReturn(Optional.of(project));
		when(categoryKeywordRepository.findById(2L)).thenReturn(Optional.of(middle));
		EntriesReplaceRequest request = new EntriesReplaceRequest(
				List.of(new EntryUpsertRequest(1L, 2L, "소분류", "수정된 상세업무", 80)));

		WeeklyReportResponse afterEdit = weeklyReportService.replaceEntries(memberPrincipal, WEEK_START,
				EntrySection.THIS_WEEK, request);

		assertThat(afterEdit.status()).isEqualTo(WeeklyReportStatus.DRAFT);
	}

	@Test
	void 이미_DRAFT인_보고서를_다시_작성해도_그대로_DRAFT() throws Exception {
		User user = newUser("멤버", Role.STAFF);
		setId(user, 5L);
		WeeklyReport report = new WeeklyReport(user, WEEK_START, WEEK_END);
		setId(report, 900L);
		when(userRepository.getReferenceById(5L)).thenReturn(user);
		when(weeklyReportRepository.findByUser_IdAndWeekStart(5L, WEEK_START)).thenReturn(Optional.of(report));
		when(weeklyReportEntryRepository.findAllByReport_IdOrderBySectionAscOrderIndexAsc(900L)).thenReturn(List.of());

		WeeklyReportResponse response = weeklyReportService.reopenMyReport(memberPrincipal, WEEK_START);

		assertThat(response.status()).isEqualTo(WeeklyReportStatus.DRAFT);
	}

	@Test
	void 팀_대시보드는_제출_인원수를_센다() throws Exception {
		AuthenticatedUser leaderPrincipal = new AuthenticatedUser(1L, 10L, "leader@growtech.io", Role.LEADER);
		User submitted = newUser("제출자", Role.STAFF);
		setId(submitted, 5L);
		User notSubmitted = newUser("미제출자", Role.STAFF);
		setId(notSubmitted, 6L);
		when(userRepository.findAllByWorkspaceIdOrderByNameAsc(10L)).thenReturn(List.of(submitted, notSubmitted));

		WeeklyReport submittedReport = new WeeklyReport(submitted, WEEK_START, WEEK_END);
		submittedReport.submit();
		setId(submittedReport, 900L);
		when(weeklyReportRepository.findAllByUser_IdInAndWeekStart(List.of(5L, 6L), WEEK_START))
				.thenReturn(List.of(submittedReport));
		when(weeklyReportEntryRepository.findAllByReport_IdInOrderBySectionAscOrderIndexAsc(List.of(900L)))
				.thenReturn(List.of());

		TeamDashboardResponse response = weeklyReportService.getTeamDashboard(leaderPrincipal, WEEK_START);

		assertThat(response.submittedCount()).isEqualTo(1);
		assertThat(response.totalMemberCount()).isEqualTo(2);
		assertThat(response.members()).hasSize(2);
	}

	@Test
	void 대표_뷰는_대분류_기준으로_그룹핑된다() throws Exception {
		AuthenticatedUser adminPrincipal = new AuthenticatedUser(1L, 10L, "admin@growtech.io", Role.ADMIN);
		User member = newUser("멤버", Role.STAFF);
		setId(member, 5L);
		when(userRepository.findAllByWorkspaceIdOrderByNameAsc(10L)).thenReturn(List.of(member));

		WeeklyReport report = new WeeklyReport(member, WEEK_START, WEEK_END);
		setId(report, 900L);
		when(weeklyReportRepository.findAllByUser_IdInAndWeekStart(List.of(5L), WEEK_START))
				.thenReturn(List.of(report));

		Project project = newProject("rCMS (당원관리)");
		setId(project, 1L);
		CategoryKeyword middle = newCategory(CategoryType.MIDDLE, "개발/구현");
		setId(middle, 2L);
		WeeklyReportEntry entry = new WeeklyReportEntry(report, EntrySection.THIS_WEEK, project, middle, "소분류",
				"상세", 50, 0);
		when(weeklyReportEntryRepository.findAllByReport_IdInOrderBySectionAscOrderIndexAsc(List.of(900L)))
				.thenReturn(List.of(entry));

		ExecutiveDashboardResponse response = weeklyReportService.getExecutiveDashboard(adminPrincipal, WEEK_START);

		assertThat(response.categories()).hasSize(1);
		assertThat(response.categories().get(0).projectName()).isEqualTo("rCMS (당원관리)");
		assertThat(response.categories().get(0).members()).hasSize(1);
		assertThat(response.categories().get(0).members().get(0).thisWeekEntries()).hasSize(1);
	}

	// ----- FR-409(보고서 내보내기) 권한 검증: "본인 또는 같은 워크스페이스 LEADER/ADMIN만 허용" -----

	@Test
	void 개인_보고서_내보내기_본인이면_허용() throws Exception {
		User owner = newUser("작성자", Role.STAFF);
		setId(owner, 5L);
		WeeklyReport report = new WeeklyReport(owner, WEEK_START, WEEK_END);
		setId(report, 900L);
		when(weeklyReportRepository.findById(900L)).thenReturn(Optional.of(report));
		when(weeklyReportEntryRepository.findAllByReport_IdOrderBySectionAscOrderIndexAsc(900L)).thenReturn(List.of());

		AuthenticatedUser ownerPrincipal = new AuthenticatedUser(5L, 10L, "author@growtech.io", Role.STAFF);
		WeeklyReportExportView view = weeklyReportService.getReportForExport(ownerPrincipal, 900L);

		assertThat(view.authorName()).isEqualTo("작성자");
		assertThat(view.report().id()).isEqualTo(900L);
	}

	@Test
	void 개인_보고서_내보내기_같은_워크스페이스_LEADER는_타인_보고서도_허용() throws Exception {
		User owner = newUser("작성자", Role.STAFF);
		setId(owner, 5L);
		WeeklyReport report = new WeeklyReport(owner, WEEK_START, WEEK_END);
		setId(report, 900L);
		when(weeklyReportRepository.findById(900L)).thenReturn(Optional.of(report));
		when(weeklyReportEntryRepository.findAllByReport_IdOrderBySectionAscOrderIndexAsc(900L)).thenReturn(List.of());

		AuthenticatedUser leaderPrincipal = new AuthenticatedUser(99L, 10L, "leader@growtech.io", Role.LEADER);
		WeeklyReportExportView view = weeklyReportService.getReportForExport(leaderPrincipal, 900L);

		assertThat(view.authorName()).isEqualTo("작성자");
	}

	@Test
	void 개인_보고서_내보내기_타인_보고서를_일반_멤버가_요청하면_404() throws Exception {
		User owner = newUser("작성자", Role.STAFF);
		setId(owner, 5L);
		WeeklyReport report = new WeeklyReport(owner, WEEK_START, WEEK_END);
		setId(report, 900L);
		when(weeklyReportRepository.findById(900L)).thenReturn(Optional.of(report));

		AuthenticatedUser otherMemberPrincipal = new AuthenticatedUser(6L, 10L, "other@growtech.io", Role.STAFF);

		assertThatThrownBy(() -> weeklyReportService.getReportForExport(otherMemberPrincipal, 900L))
				.isInstanceOf(WeeklyReportNotFoundException.class);
	}

	@Test
	void 개인_보고서_내보내기_다른_워크스페이스면_404() throws Exception {
		Workspace otherWorkspace = new Workspace("다른회사", "other.io");
		setId(otherWorkspace, 20L);
		User owner = new User(otherWorkspace, "author@other.io", "hash", "작성자", Role.STAFF);
		setId(owner, 5L);
		WeeklyReport report = new WeeklyReport(owner, WEEK_START, WEEK_END);
		setId(report, 901L);
		when(weeklyReportRepository.findById(901L)).thenReturn(Optional.of(report));

		AuthenticatedUser adminPrincipal = new AuthenticatedUser(1L, 10L, "admin@growtech.io", Role.ADMIN);

		assertThatThrownBy(() -> weeklyReportService.getReportForExport(adminPrincipal, 901L))
				.isInstanceOf(WeeklyReportNotFoundException.class);
	}

	@Test
	void 팀_보고서_내보내기는_weekStart로_실시간_집계된다() throws Exception {
		AuthenticatedUser adminPrincipal = new AuthenticatedUser(1L, 10L, "admin@growtech.io", Role.ADMIN);
		when(userRepository.findAllByWorkspaceIdOrderByNameAsc(10L)).thenReturn(List.of());

		TeamWeeklyReportExportView view = weeklyReportService.getTeamReportForExport(adminPrincipal, WEEK_START);

		assertThat(view.report().weekStart()).isEqualTo(WEEK_START);
		assertThat(view.report().members()).isEmpty();
	}

	private User newUser(String name, Role role) {
		return new User(workspace, name.toLowerCase() + "@growtech.io", "hash", name, role);
	}

	private CategoryKeyword newCategory(CategoryType type, String name) {
		return new CategoryKeyword(type, name, 0, null);
	}

	private Project newProject(String name) {
		return new Project(workspace, name, null, null);
	}

	private void setId(Object entity, Long id) throws Exception {
		Field idField = findIdField(entity.getClass());
		idField.setAccessible(true);
		idField.set(entity, id);
	}

	private Field findIdField(Class<?> type) throws NoSuchFieldException {
		try {
			return type.getDeclaredField("id");
		} catch (NoSuchFieldException e) {
			if (type.getSuperclass() == null) {
				throw e;
			}
			return findIdField(type.getSuperclass());
		}
	}
}
