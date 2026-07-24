package com.teamsync.back.report;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.DuplicateReportTemplateException;
import com.teamsync.back.common.exception.DuplicateReportTemplateSectionException;
import com.teamsync.back.common.exception.InvalidReportRequestException;
import com.teamsync.back.common.exception.LastReportTemplateSectionException;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.common.exception.ReportTemplateNotFoundException;
import com.teamsync.back.common.exception.ReportTemplateSectionNotFoundException;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.report.dto.ReportTemplateCreateRequest;
import com.teamsync.back.report.dto.ReportTemplateResponse;
import com.teamsync.back.report.dto.ReportTemplateScope;
import com.teamsync.back.report.dto.ReportTemplateSectionCreateRequest;
import com.teamsync.back.report.dto.ReportTemplateSectionOrderRequest;
import com.teamsync.back.report.dto.ReportTemplateSectionResponse;
import com.teamsync.back.report.dto.ReportTemplateSectionUpdateRequest;
import com.teamsync.back.report.dto.ReportTemplateUpdateRequest;
import com.teamsync.back.workspace.Workspace;
import com.teamsync.back.workspace.WorkspaceRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-405(P3, 보고서 템플릿 관리): 템플릿(양식)의 CRUD + 섹션 추가/삭제/순서변경만 다룬다.
 * 이번 세션은 의도된 축소 범위로, 여기서 만든 템플릿이 실제 주간보고 작성/제출 화면(FR-401~404,
 * WeeklyReportService)에 즉시 반영되지는 않는다(후속 과제, HANDOFF 한계 항목).
 */
@Service
public class ReportTemplateService {

	// 표준 섹션(MANUAL 제외) 4종의 기본 한국어 제목 + 생성 순서. FE 정적 목업(admin-templates,
	// report/editor)에서 이미 쓰인 표현을 그대로 재사용해 일관성을 맞춘다.
	private static final List<ReportSectionKey> DEFAULT_SECTION_KEYS = List.of(
			ReportSectionKey.COMPLETED, ReportSectionKey.IN_PROGRESS, ReportSectionKey.HIGHLIGHTS,
			ReportSectionKey.ISSUES);

	private final ReportTemplateRepository reportTemplateRepository;
	private final ReportTemplateSectionRepository reportTemplateSectionRepository;
	private final ProjectRepository projectRepository;
	private final WorkspaceRepository workspaceRepository;

	public ReportTemplateService(ReportTemplateRepository reportTemplateRepository,
			ReportTemplateSectionRepository reportTemplateSectionRepository, ProjectRepository projectRepository,
			WorkspaceRepository workspaceRepository) {
		this.reportTemplateRepository = reportTemplateRepository;
		this.reportTemplateSectionRepository = reportTemplateSectionRepository;
		this.projectRepository = projectRepository;
		this.workspaceRepository = workspaceRepository;
	}

	private static String defaultTitleOf(ReportSectionKey key) {
		return switch (key) {
			case COMPLETED -> "완료한 일";
			case IN_PROGRESS -> "진행 중인 일";
			case HIGHLIGHTS -> "이번 주 하이라이트";
			case ISSUES -> "이슈 및 리스크";
			case MANUAL -> "새 섹션";
		};
	}

	// ----- 조회(resolve): 인증만 필요, 전원 -----

	/**
	 * 우선순위: 프로젝트 전용 템플릿 > 워크스페이스 전사 기본 템플릿 > 비영속 가상 SYSTEM_DEFAULT.
	 * SYSTEM_DEFAULT는 id:null이며 4개 표준 섹션(모두 autoFilled=true)을 한국어 기본 제목으로 채워
	 * 그 자리에서 만들어 반환한다(DB에 저장하지 않음).
	 */
	@Transactional(readOnly = true)
	public ReportTemplateResponse resolveTemplate(AuthenticatedUser principal, Long projectId) {
		Project project = getProjectInWorkspace(principal, projectId);

		Optional<ReportTemplate> projectTemplate = reportTemplateRepository.findByProject_Id(project.getId());
		if (projectTemplate.isPresent()) {
			return toResponse(projectTemplate.get(), ReportTemplateScope.PROJECT);
		}

		Optional<ReportTemplate> workspaceTemplate = reportTemplateRepository
				.findByWorkspace_IdAndProjectIsNull(principal.workspaceId());
		if (workspaceTemplate.isPresent()) {
			return toResponse(workspaceTemplate.get(), ReportTemplateScope.WORKSPACE);
		}

		return systemDefaultResponse(projectId);
	}

	private ReportTemplateResponse systemDefaultResponse(Long projectId) {
		List<ReportTemplateSectionResponse> sections = new ArrayList<>();
		int orderIndex = 0;
		for (ReportSectionKey key : DEFAULT_SECTION_KEYS) {
			sections.add(new ReportTemplateSectionResponse(null, key, defaultTitleOf(key), orderIndex++, true));
		}
		return new ReportTemplateResponse(null, ReportTemplateScope.SYSTEM_DEFAULT, projectId, "기본 템플릿", sections);
	}

	// ----- 관리(ADMIN/LEADER) -----

	@Transactional(readOnly = true)
	public ReportTemplateResponse getWorkspaceTemplate(AuthenticatedUser principal) {
		return reportTemplateRepository.findByWorkspace_IdAndProjectIsNull(principal.workspaceId())
				.map(t -> toResponse(t, ReportTemplateScope.WORKSPACE))
				.orElse(null);
	}

	@Transactional(readOnly = true)
	public ReportTemplateResponse getProjectTemplate(AuthenticatedUser principal, Long projectId) {
		Project project = getProjectInWorkspace(principal, projectId);
		return reportTemplateRepository.findByProject_Id(project.getId())
				.map(t -> toResponse(t, ReportTemplateScope.PROJECT))
				.orElse(null);
	}

	@Transactional
	public ReportTemplateResponse createTemplate(AuthenticatedUser principal, ReportTemplateCreateRequest request) {
		Workspace workspace = workspaceRepository.getReferenceById(principal.workspaceId());
		Project project = null;
		ReportTemplateScope scope;

		if (request.projectId() == null) {
			if (reportTemplateRepository.findByWorkspace_IdAndProjectIsNull(principal.workspaceId()).isPresent()) {
				throw new DuplicateReportTemplateException();
			}
			scope = ReportTemplateScope.WORKSPACE;
		} else {
			project = getProjectInWorkspace(principal, request.projectId());
			if (reportTemplateRepository.findByProject_Id(project.getId()).isPresent()) {
				throw new DuplicateReportTemplateException();
			}
			scope = ReportTemplateScope.PROJECT;
		}

		ReportTemplate template = reportTemplateRepository
				.save(new ReportTemplate(workspace, project, request.name().trim()));
		seedDefaultSections(template);

		return toResponse(template, scope);
	}

	private void seedDefaultSections(ReportTemplate template) {
		int orderIndex = 0;
		for (ReportSectionKey key : DEFAULT_SECTION_KEYS) {
			reportTemplateSectionRepository
					.save(new ReportTemplateSection(template, key, defaultTitleOf(key), orderIndex++));
		}
	}

	@Transactional
	public ReportTemplateResponse updateTemplateName(AuthenticatedUser principal, Long templateId,
			ReportTemplateUpdateRequest request) {
		ReportTemplate template = getTemplateInWorkspace(principal, templateId);
		template.changeName(request.name().trim());
		return toResponse(template, scopeOf(template));
	}

	@Transactional
	public void deleteTemplate(AuthenticatedUser principal, Long templateId) {
		ReportTemplate template = getTemplateInWorkspace(principal, templateId);
		// 섹션은 report_template_sections.template_id ON DELETE CASCADE(V20)로 함께 삭제된다.
		reportTemplateRepository.delete(template);
	}

	@Transactional
	public ReportTemplateResponse addSection(AuthenticatedUser principal, Long templateId,
			ReportTemplateSectionCreateRequest request) {
		ReportTemplate template = getTemplateInWorkspace(principal, templateId);

		if (request.sectionKey() != ReportSectionKey.MANUAL
				&& reportTemplateSectionRepository.existsByTemplate_IdAndSectionKey(templateId, request.sectionKey())) {
			throw new DuplicateReportTemplateSectionException();
		}

		List<ReportTemplateSection> existing = reportTemplateSectionRepository
				.findAllByTemplate_IdOrderByOrderIndexAsc(templateId);
		int nextOrderIndex = existing.isEmpty() ? 0
				: existing.get(existing.size() - 1).getOrderIndex() + 1;

		reportTemplateSectionRepository
				.save(new ReportTemplateSection(template, request.sectionKey(), request.title().trim(), nextOrderIndex));

		return toResponse(template, scopeOf(template));
	}

	@Transactional
	public ReportTemplateResponse updateSectionTitle(AuthenticatedUser principal, Long templateId, Long sectionId,
			ReportTemplateSectionUpdateRequest request) {
		ReportTemplate template = getTemplateInWorkspace(principal, templateId);
		ReportTemplateSection section = getSectionInTemplate(templateId, sectionId);
		section.changeTitle(request.title().trim());
		return toResponse(template, scopeOf(template));
	}

	@Transactional
	public ReportTemplateResponse deleteSection(AuthenticatedUser principal, Long templateId, Long sectionId) {
		ReportTemplate template = getTemplateInWorkspace(principal, templateId);
		ReportTemplateSection section = getSectionInTemplate(templateId, sectionId);

		if (reportTemplateSectionRepository.countByTemplate_Id(templateId) <= 1) {
			throw new LastReportTemplateSectionException();
		}

		reportTemplateSectionRepository.delete(section);
		return toResponse(template, scopeOf(template));
	}

	/**
	 * sectionIds는 해당 템플릿의 전체 섹션 id 집합과 정확히 일치해야 한다(누락/추가/타 템플릿 id
	 * 혼입 시 400). 순서대로 0..n-1 orderIndex를 재할당한다.
	 */
	@Transactional
	public ReportTemplateResponse reorderSections(AuthenticatedUser principal, Long templateId,
			ReportTemplateSectionOrderRequest request) {
		ReportTemplate template = getTemplateInWorkspace(principal, templateId);
		List<ReportTemplateSection> existing = reportTemplateSectionRepository
				.findAllByTemplate_IdOrderByOrderIndexAsc(templateId);

		Set<Long> existingIds = new LinkedHashSet<>();
		existing.forEach(s -> existingIds.add(s.getId()));
		Set<Long> requestedIds = new LinkedHashSet<>(request.sectionIds());

		if (requestedIds.size() != request.sectionIds().size() || !requestedIds.equals(existingIds)) {
			throw new InvalidReportRequestException("sectionIds가 템플릿의 전체 섹션 집합과 일치하지 않습니다.");
		}

		Map<Long, ReportTemplateSection> byId = new HashMap<>();
		existing.forEach(s -> byId.put(s.getId(), s));

		int orderIndex = 0;
		for (Long sectionId : request.sectionIds()) {
			byId.get(sectionId).changeOrderIndex(orderIndex++);
		}

		return toResponse(template, scopeOf(template));
	}

	// ----- 내부 구현 -----

	private ReportTemplateResponse toResponse(ReportTemplate template, ReportTemplateScope scope) {
		List<ReportTemplateSectionResponse> sections = reportTemplateSectionRepository
				.findAllByTemplate_IdOrderByOrderIndexAsc(template.getId()).stream()
				.map(ReportTemplateSectionResponse::from)
				.toList();
		Long projectId = template.getProject() != null ? template.getProject().getId() : null;
		return new ReportTemplateResponse(template.getId(), scope, projectId, template.getName(), sections);
	}

	private ReportTemplateScope scopeOf(ReportTemplate template) {
		return template.getProject() != null ? ReportTemplateScope.PROJECT : ReportTemplateScope.WORKSPACE;
	}

	private Project getProjectInWorkspace(AuthenticatedUser principal, Long projectId) {
		return projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
	}

	private ReportTemplate getTemplateInWorkspace(AuthenticatedUser principal, Long templateId) {
		return reportTemplateRepository.findByIdAndWorkspace_Id(templateId, principal.workspaceId())
				.orElseThrow(ReportTemplateNotFoundException::new);
	}

	private ReportTemplateSection getSectionInTemplate(Long templateId, Long sectionId) {
		return reportTemplateSectionRepository.findByIdAndTemplate_Id(sectionId, templateId)
				.orElseThrow(ReportTemplateSectionNotFoundException::new);
	}
}
