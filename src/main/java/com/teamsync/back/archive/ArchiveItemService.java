package com.teamsync.back.archive;

import com.teamsync.back.archive.dto.ArchiveItemCreateRequest;
import com.teamsync.back.archive.dto.ArchiveItemResponse;
import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-205 정보 아카이브 서비스. TaskService/ChannelService와 동일한 원칙: 클라이언트가 전달한
 * projectId가 요청자의 워크스페이스에 실제로 속하는지 항상 principal.workspaceId() 기준으로
 * 재검증하고, 아니면 404로 응답해 다른 워크스페이스 데이터의 존재 자체를 숨긴다(PRD 5.6 리스크 대응).
 */
@Service
public class ArchiveItemService {

	private final ArchiveItemRepository archiveItemRepository;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;

	public ArchiveItemService(ArchiveItemRepository archiveItemRepository, ProjectRepository projectRepository,
			UserRepository userRepository) {
		this.archiveItemRepository = archiveItemRepository;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<ArchiveItemResponse> listArchiveItems(AuthenticatedUser principal, Long projectId) {
		getProjectInWorkspace(principal, projectId);
		return archiveItemRepository.findAllByProject_IdOrderByCreatedAtDescIdDesc(projectId).stream()
				.map(ArchiveItemResponse::from)
				.toList();
	}

	@Transactional
	public ArchiveItemResponse createArchiveItem(AuthenticatedUser principal, Long projectId,
			ArchiveItemCreateRequest request) {
		Project project = getProjectInWorkspace(principal, projectId);
		User author = userRepository.getReferenceById(principal.userId());

		ArchiveItem archiveItem = new ArchiveItem(
				project,
				request.type(),
				request.title().trim(),
				request.content().trim(),
				author,
				normalizeTags(request.tags()));

		return ArchiveItemResponse.from(archiveItemRepository.save(archiveItem));
	}

	private static List<String> normalizeTags(List<String> tags) {
		if (tags == null) {
			return List.of();
		}
		return tags.stream()
				.filter(tag -> tag != null && !tag.isBlank())
				.map(String::trim)
				.toList();
	}

	private Project getProjectInWorkspace(AuthenticatedUser principal, Long projectId) {
		return projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
	}
}
