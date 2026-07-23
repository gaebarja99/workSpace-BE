package com.teamsync.back.archive.file;

import com.teamsync.back.archive.file.dto.ArchivedFileResponse;
import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.common.exception.ArchivedFileNotFoundException;
import com.teamsync.back.common.exception.InvalidFileUploadException;
import com.teamsync.back.common.exception.ProjectNotFoundException;
import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import com.teamsync.back.storage.FileStorageService;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * FR-204 파일 아카이브 서비스. TaskService/ChannelService와 동일한 원칙: projectId/fileId가
 * 요청자 워크스페이스에 실제로 속하는지 항상 재검증하고, 아니면 404로 응답해 다른 워크스페이스
 * 데이터의 존재 자체를 숨긴다(PRD 5.6 리스크 대응). 실제 바이트 저장/조회는 FileStorageService
 * (현재는 로컬 디스크 구현체)에 위임해 인프라 교체 시 이 서비스는 변경되지 않도록 한다.
 */
@Service
public class ArchivedFileService {

	private final ArchivedFileRepository archivedFileRepository;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final FileStorageService fileStorageService;

	public ArchivedFileService(ArchivedFileRepository archivedFileRepository, ProjectRepository projectRepository,
			UserRepository userRepository, FileStorageService fileStorageService) {
		this.archivedFileRepository = archivedFileRepository;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
		this.fileStorageService = fileStorageService;
	}

	@Transactional(readOnly = true)
	public List<ArchivedFileResponse> listFiles(AuthenticatedUser principal, Long projectId) {
		getProjectInWorkspace(principal, projectId);
		return archivedFileRepository.findAllByProject_IdOrderByCreatedAtDescIdDesc(projectId).stream()
				.map(ArchivedFileResponse::from)
				.toList();
	}

	@Transactional
	public ArchivedFileResponse uploadFile(AuthenticatedUser principal, Long projectId, MultipartFile file,
			String tags) {
		Project project = getProjectInWorkspace(principal, projectId);
		if (file == null || file.isEmpty()) {
			throw new InvalidFileUploadException("업로드할 파일이 비어 있습니다.");
		}
		String originalFilename = file.getOriginalFilename();
		if (originalFilename == null || originalFilename.isBlank()) {
			throw new InvalidFileUploadException("파일명이 없습니다.");
		}
		User uploader = userRepository.getReferenceById(principal.userId());

		// 실제 바이트 저장을 먼저 수행한 뒤 메타데이터를 커밋한다. 디스크 쓰기가 실패하면 예외가 트랜잭션을
		// 롤백시켜 메타데이터만 존재하는 상태(고아 레코드)를 방지한다.
		String storageKey = fileStorageService.store(file);

		ArchivedFile archivedFile = new ArchivedFile(
				project,
				originalFilename,
				file.getContentType(),
				file.getSize(),
				storageKey,
				uploader,
				normalizeTags(tags));

		return ArchivedFileResponse.from(archivedFileRepository.save(archivedFile));
	}

	@Transactional(readOnly = true)
	public ArchivedFile getFileForDownload(AuthenticatedUser principal, Long fileId) {
		return archivedFileRepository.findByIdAndProject_Workspace_Id(fileId, principal.workspaceId())
				.orElseThrow(ArchivedFileNotFoundException::new);
	}

	public Resource loadFileResource(String storageKey) {
		return fileStorageService.load(storageKey);
	}

	private static List<String> normalizeTags(String tags) {
		if (tags == null || tags.isBlank()) {
			return List.of();
		}
		return Arrays.stream(tags.split(","))
				.map(String::trim)
				.filter(tag -> !tag.isEmpty())
				.toList();
	}

	private Project getProjectInWorkspace(AuthenticatedUser principal, Long projectId) {
		return projectRepository.findByIdAndWorkspaceId(projectId, principal.workspaceId())
				.orElseThrow(ProjectNotFoundException::new);
	}
}
