package com.teamsync.back.task.recurrence;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.InvalidAssigneeException;
import com.teamsync.back.common.exception.InvalidRecurringTaskTemplateRequestException;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.common.exception.RecurringTaskTemplateNotFoundException;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.task.TaskPriority;
import com.teamsync.back.task.recurrence.dto.RecurringTaskTemplateCreateRequest;
import com.teamsync.back.task.recurrence.dto.RecurringTaskTemplateResponse;
import com.teamsync.back.task.recurrence.dto.RecurringTaskTemplateUpdateRequest;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.time.DayOfWeek;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-106(반복 태스크 템플릿, 주간/월간 자동 생성): 이 서비스는 "생성 규칙"(템플릿)의 CRUD만
 * 담당하고, 실제 Task 생성은 RecurringTaskSchedulerService의 일 배치가 전담한다.
 * TaskService와 동일한 원칙으로 projectId/templateId가 요청자의 워크스페이스에 실제로 속하는지
 * 항상 principal.workspaceId() 기준으로 재검증한다(PRD 5.6 리스크 대응).
 */
@Service
public class RecurringTaskTemplateService {

	private final RecurringTaskTemplateRepository recurringTaskTemplateRepository;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;

	public RecurringTaskTemplateService(RecurringTaskTemplateRepository recurringTaskTemplateRepository,
			ProjectRepository projectRepository, UserRepository userRepository) {
		this.recurringTaskTemplateRepository = recurringTaskTemplateRepository;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public RecurringTaskTemplateResponse createTemplate(AuthenticatedUser principal, Long projectId,
			RecurringTaskTemplateCreateRequest request) {
		Project project = getProjectInWorkspace(principal, projectId);
		validateRecurrence(request.recurrenceType(), request.dayOfWeek(), request.dayOfMonth());
		Set<User> assignees = resolveAssignees(principal, request.assigneeIds());
		User createdBy = userRepository.getReferenceById(principal.userId());

		RecurringTaskTemplate template = new RecurringTaskTemplate(
				project,
				request.title().trim(),
				request.description(),
				request.priority() != null ? request.priority() : TaskPriority.MEDIUM,
				assignees,
				request.recurrenceType(),
				request.dayOfWeek(),
				request.dayOfMonth(),
				request.dueInDays(),
				request.active() != null ? request.active() : true,
				createdBy);

		return RecurringTaskTemplateResponse.from(recurringTaskTemplateRepository.save(template));
	}

	@Transactional(readOnly = true)
	public List<RecurringTaskTemplateResponse> listTemplates(AuthenticatedUser principal, Long projectId) {
		getProjectInWorkspace(principal, projectId);
		return recurringTaskTemplateRepository.findAllByProject_IdOrderByIdAsc(projectId).stream()
				.map(RecurringTaskTemplateResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public RecurringTaskTemplateResponse getTemplate(AuthenticatedUser principal, Long templateId) {
		return RecurringTaskTemplateResponse.from(getTemplateInWorkspace(principal, templateId));
	}

	@Transactional
	public RecurringTaskTemplateResponse updateTemplate(AuthenticatedUser principal, Long templateId,
			RecurringTaskTemplateUpdateRequest request) {
		RecurringTaskTemplate template = getTemplateInWorkspace(principal, templateId);

		if (request.title() != null) {
			String trimmed = request.title().trim();
			if (trimmed.isEmpty()) {
				throw new InvalidRecurringTaskTemplateRequestException("템플릿 제목은 공백일 수 없습니다.");
			}
			template.changeTitle(trimmed);
		}
		if (request.description() != null) {
			template.changeDescription(request.description());
		}
		if (request.priority() != null) {
			template.changePriority(request.priority());
		}
		if (request.assigneeIds() != null) {
			if (request.assigneeIds().isEmpty()) {
				throw new InvalidRecurringTaskTemplateRequestException("담당자는 최소 1명 이상이어야 합니다.");
			}
			template.changeAssignees(resolveAssignees(principal, request.assigneeIds()));
		}
		if (request.dueInDays() != null) {
			template.changeDueInDays(request.dueInDays());
		}
		if (request.active() != null) {
			template.changeActive(request.active());
		}
		applyRecurrenceUpdateIfPresent(template, request);

		return RecurringTaskTemplateResponse.from(template);
	}

	@Transactional
	public void deleteTemplate(AuthenticatedUser principal, Long templateId) {
		RecurringTaskTemplate template = getTemplateInWorkspace(principal, templateId);
		// 하드 삭제. tasks.recurring_template_id는 ON DELETE SET NULL(V15)이라 이미 생성된 Task는
		// 그대로 유지되고 유래 템플릿 참조만 사라진다.
		recurringTaskTemplateRepository.delete(template);
	}

	/**
	 * recurrenceType/dayOfWeek/dayOfMonth 중 하나라도 요청에 포함된 경우에만 재계산하고, 아무것도
	 * 오지 않으면 기존 값을 그대로 둔다. recurrenceType이 바뀌는 경우 그 주기에 맞는 day 필드가 같은
	 * 요청 안에 함께 와야 하며(그렇지 않으면 검증 실패), 반대쪽 필드는 자동으로 null 처리한다. 그렇지
	 * 않고 recurrenceType 변경 없이 day 필드만 오면 기존 recurrenceType 기준으로 최종 상태를 검증한다
	 * (예: 기존 WEEKLY 템플릿에 dayOfMonth만 보내면 상호배타 규칙 위반으로 400).
	 */
	private void applyRecurrenceUpdateIfPresent(RecurringTaskTemplate template,
			RecurringTaskTemplateUpdateRequest request) {
		if (request.recurrenceType() == null && request.dayOfWeek() == null && request.dayOfMonth() == null) {
			return;
		}

		RecurrenceType resolvedType = request.recurrenceType() != null ? request.recurrenceType()
				: template.getRecurrenceType();
		DayOfWeek resolvedDayOfWeek;
		Integer resolvedDayOfMonth;
		if (request.recurrenceType() != null) {
			resolvedDayOfWeek = resolvedType == RecurrenceType.WEEKLY ? request.dayOfWeek() : null;
			resolvedDayOfMonth = resolvedType == RecurrenceType.MONTHLY ? request.dayOfMonth() : null;
		} else {
			resolvedDayOfWeek = request.dayOfWeek() != null ? request.dayOfWeek() : template.getDayOfWeek();
			resolvedDayOfMonth = request.dayOfMonth() != null ? request.dayOfMonth() : template.getDayOfMonth();
		}

		validateRecurrence(resolvedType, resolvedDayOfWeek, resolvedDayOfMonth);
		template.changeRecurrence(resolvedType, resolvedDayOfWeek, resolvedDayOfMonth);
	}

	/** recurrenceType=WEEKLY <-> dayOfWeek 필수/dayOfMonth 반드시 null, MONTHLY <-> 그 반대(상호배타). */
	private void validateRecurrence(RecurrenceType recurrenceType, DayOfWeek dayOfWeek, Integer dayOfMonth) {
		if (recurrenceType == RecurrenceType.WEEKLY) {
			if (dayOfWeek == null) {
				throw new InvalidRecurringTaskTemplateRequestException("WEEKLY 반복은 dayOfWeek가 필수입니다.");
			}
			if (dayOfMonth != null) {
				throw new InvalidRecurringTaskTemplateRequestException("WEEKLY 반복에는 dayOfMonth를 지정할 수 없습니다.");
			}
			return;
		}

		if (dayOfMonth == null) {
			throw new InvalidRecurringTaskTemplateRequestException("MONTHLY 반복은 dayOfMonth가 필수입니다.");
		}
		if (dayOfMonth < 1 || dayOfMonth > 31) {
			throw new InvalidRecurringTaskTemplateRequestException("dayOfMonth는 1~31 사이여야 합니다.");
		}
		if (dayOfWeek != null) {
			throw new InvalidRecurringTaskTemplateRequestException("MONTHLY 반복에는 dayOfWeek를 지정할 수 없습니다.");
		}
	}

	private Project getProjectInWorkspace(AuthenticatedUser principal, Long projectId) {
		return projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
	}

	private RecurringTaskTemplate getTemplateInWorkspace(AuthenticatedUser principal, Long templateId) {
		return recurringTaskTemplateRepository.findByIdAndProject_Workspace_Id(templateId, principal.workspaceId())
				.orElseThrow(RecurringTaskTemplateNotFoundException::new);
	}

	// TaskService.resolveAssignees와 동일 원칙: 프로젝트 멤버 개념이 워크스페이스 소속과 동일하므로
	// 같은 방식(워크스페이스 소속 검증)으로 "프로젝트 멤버 검증"을 수행한다.
	private Set<User> resolveAssignees(AuthenticatedUser principal, List<Long> assigneeIds) {
		Set<Long> distinctIds = new LinkedHashSet<>(assigneeIds);
		List<User> users = userRepository.findAllByIdInAndWorkspaceId(distinctIds, principal.workspaceId());
		if (users.size() != distinctIds.size()) {
			throw new InvalidAssigneeException();
		}
		return new LinkedHashSet<>(users);
	}
}
