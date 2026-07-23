package com.teamsync.back.archive.file;

import com.teamsync.back.archive.file.dto.ArchivedFileResponse;
import com.teamsync.back.auth.AuthenticatedUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * FR-204(파일 아카이브) API.
 * 목록 조회(GET)는 인증된 워크스페이스 구성원이면 GUEST를 포함해 누구나 가능하고,
 * 업로드(POST)는 TaskController/ChannelController와 동일하게 GUEST를 제외한 ADMIN/LEADER/MEMBER만 가능하다.
 * 다운로드는 fileId가 요청자 워크스페이스 소속 프로젝트에 속하는 한 인증된 누구나 가능하다(역할 제한 없음).
 */
@RestController
public class ArchivedFileController {

	private final ArchivedFileService archivedFileService;

	public ArchivedFileController(ArchivedFileService archivedFileService) {
		this.archivedFileService = archivedFileService;
	}

	@GetMapping("/api/projects/{projectId}/files")
	public ResponseEntity<List<ArchivedFileResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId) {
		return ResponseEntity.ok(archivedFileService.listFiles(principal, projectId));
	}

	@PostMapping(value = "/api/projects/{projectId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<ArchivedFileResponse> upload(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @RequestParam("file") MultipartFile file,
			@RequestParam(value = "tags", required = false) String tags) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(archivedFileService.uploadFile(principal, projectId, file, tags));
	}

	@GetMapping("/api/files/{fileId}/download")
	public ResponseEntity<Resource> download(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long fileId) {
		ArchivedFile archivedFile = archivedFileService.getFileForDownload(principal, fileId);
		Resource resource = archivedFileService.loadFileResource(archivedFile.getStorageKey());

		MediaType mediaType = resolveMediaType(archivedFile.getContentType());
		ContentDisposition contentDisposition = ContentDisposition.attachment()
				.filename(archivedFile.getOriginalFilename(), StandardCharsets.UTF_8)
				.build();

		return ResponseEntity.ok()
				.contentType(mediaType)
				.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
				.body(resource);
	}

	private static MediaType resolveMediaType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
		try {
			return MediaType.parseMediaType(contentType);
		} catch (IllegalArgumentException e) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}
}
