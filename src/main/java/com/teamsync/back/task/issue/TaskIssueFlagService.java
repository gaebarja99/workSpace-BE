package com.teamsync.back.task.issue;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.common.exception.TaskIssueFlagNotFoundException;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.task.issue.dto.TaskIssueItemResponse;
import com.teamsync.back.task.issue.dto.TaskIssueListResponse;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-406(이슈/리스크 자동 플래그, 완전판) API 서비스. ReportController/TaskDependencyService와
 * 동일한 원칙으로 projectId가 요청자의 워크스페이스 소속인지 항상 재검증한다(PRD 5.6 리스크 대응).
 */
@Service
public class TaskIssueFlagService {

	private final ProjectRepository projectRepository;
	private final TaskIssueFlagRepository taskIssueFlagRepository;
	private final UserRepository userRepository;

	public TaskIssueFlagService(ProjectRepository projectRepository, TaskIssueFlagRepository taskIssueFlagRepository,
			UserRepository userRepository) {
		this.projectRepository = projectRepository;
		this.taskIssueFlagRepository = taskIssueFlagRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public TaskIssueListResponse listIssues(AuthenticatedUser principal, Long projectId, TaskIssueStatus status,
			TaskIssueKind kind) {
		Project project = getProjectInWorkspace(principal, projectId);
		TaskIssueStatus resolvedStatus = status != null ? status : TaskIssueStatus.OPEN;

		var flags = kind != null
				? taskIssueFlagRepository.findAllByTask_Project_IdAndStatusAndKindOrderByDetectedAtDesc(
						project.getId(), resolvedStatus, kind)
				: taskIssueFlagRepository.findAllByTask_Project_IdAndStatusOrderByDetectedAtDesc(
						project.getId(), resolvedStatus);

		return new TaskIssueListResponse(flags.stream().map(TaskIssueItemResponse::from).toList());
	}

	@Transactional
	public TaskIssueItemResponse resolveIssue(AuthenticatedUser principal, Long projectId, Long issueId) {
		Project project = getProjectInWorkspace(principal, projectId);
		TaskIssueFlag flag = taskIssueFlagRepository.findByIdAndTask_Project_Id(issueId, project.getId())
				.orElseThrow(TaskIssueFlagNotFoundException::new);

		// 이미 RESOLVED면 그대로 현재 상태를 반환한다(계약: 에러 아님, resolve()가 자체적으로 no-op 처리).
		if (flag.getStatus() != TaskIssueStatus.RESOLVED) {
			User resolvedBy = userRepository.getReferenceById(principal.userId());
			flag.resolve(resolvedBy);
		}
		return TaskIssueItemResponse.from(flag);
	}

	private Project getProjectInWorkspace(AuthenticatedUser principal, Long projectId) {
		return projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
	}
}
