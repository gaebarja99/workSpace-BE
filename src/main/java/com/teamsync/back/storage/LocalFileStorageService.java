package com.teamsync.back.storage;

import com.teamsync.back.common.exception.ArchivedFileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * FR-204 파일 아카이브 로컬 디스크 저장 구현체(현재 유일한 구현체, 후속 S3 교체 대상).
 * 원본 파일명을 디스크 경로에 그대로 쓰지 않고 UUID 기반 내부 파일명(storageKey)을 생성해
 * 파일명 충돌 및 경로 순회(path traversal)를 방지한다. 원본 파일명은 ArchivedFile 엔티티(DB)에만
 * 메타데이터로 보관한다.
 */
@Service
public class LocalFileStorageService implements FileStorageService {

	private final Path rootDirectory;

	public LocalFileStorageService(StorageProperties storageProperties) {
		this.rootDirectory = Path.of(storageProperties.localBasePath()).toAbsolutePath().normalize();
		try {
			Files.createDirectories(rootDirectory);
		} catch (IOException e) {
			throw new UncheckedIOException("파일 저장소 디렉터리를 생성할 수 없습니다: " + rootDirectory, e);
		}
	}

	@Override
	public String store(MultipartFile file) {
		String storageKey = UUID.randomUUID().toString();
		Path target = resolveWithinRoot(storageKey);
		try {
			file.transferTo(target);
		} catch (IOException e) {
			throw new UncheckedIOException("파일 저장에 실패했습니다.", e);
		}
		return storageKey;
	}

	@Override
	public Resource load(String storageKey) {
		Path target = resolveWithinRoot(storageKey);
		if (!Files.exists(target)) {
			throw new ArchivedFileNotFoundException();
		}
		try {
			Resource resource = new UrlResource(target.toUri());
			if (!resource.exists() || !resource.isReadable()) {
				throw new ArchivedFileNotFoundException();
			}
			return resource;
		} catch (MalformedURLException e) {
			throw new ArchivedFileNotFoundException();
		}
	}

	/**
	 * storageKey는 항상 store()가 생성한 UUID이므로 정상 흐름에서는 이 검증에 걸릴 수 없지만,
	 * 호출 경로가 늘어나도 경로 순회를 원천 차단하기 위해 방어적으로 rootDirectory 하위인지 매번 확인한다.
	 */
	private Path resolveWithinRoot(String storageKey) {
		Path target = rootDirectory.resolve(storageKey).normalize();
		if (!target.getParent().equals(rootDirectory)) {
			throw new ArchivedFileNotFoundException();
		}
		return target;
	}
}
