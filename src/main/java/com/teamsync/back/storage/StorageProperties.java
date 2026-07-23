package com.teamsync.back.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FR-204 파일 아카이브 저장소 설정. 현재는 로컬 디스크만 지원하며(docker-compose에 S3/오브젝트
 * 스토리지 인프라가 없음), localBasePath만 외부화해두어 후속에 S3 등으로 교체할 때 이 프로퍼티를
 * 읽는 지점(LocalFileStorageService)만 교체하면 되도록 한다(PRD 5.1 모듈형 모놀리스, 후속 확장 원칙).
 * BackApplication의 @ConfigurationPropertiesScan으로 자동 등록되므로 별도의
 * @EnableConfigurationProperties 선언은 필요 없다(JwtProperties와 동일한 컨벤션).
 */
@ConfigurationProperties(prefix = "teamsync.storage")
public record StorageProperties(
		String localBasePath
) {
}
