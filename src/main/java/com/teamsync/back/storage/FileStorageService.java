package com.teamsync.back.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * FR-204 파일 아카이브 저장소 추상화. 현재 구현체는 로컬 디스크(LocalFileStorageService)뿐이지만,
 * 이 인터페이스만 유지하면 후속에 S3 등 오브젝트 스토리지 구현체로 교체할 수 있다
 * (PRD 5.1 모듈형 모놀리스 확장 원칙).
 */
public interface FileStorageService {

	/**
	 * 업로드된 파일을 저장하고, 내부적으로 생성한 저장 키(원본 파일명과 무관한 UUID 기반 —
	 * 경로 순회/파일명 충돌 방지 목적)를 반환한다. 원본 파일명은 호출자가 DB 메타데이터로 별도 보관한다.
	 */
	String store(MultipartFile file);

	/**
	 * 저장 키로 파일 리소스를 읽어들인다. storageKey는 항상 store()가 생성한 값만 사용해야 하며,
	 * 사용자 입력을 직접 조합해 호출해서는 안 된다(경로 순회 방지).
	 */
	Resource load(String storageKey);
}
