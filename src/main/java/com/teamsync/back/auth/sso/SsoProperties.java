package com.teamsync.back.auth.sso;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FR-002 SSO 공급자 설정. BackApplication의 @ConfigurationPropertiesScan으로 자동 등록된다
 * (JwtProperties/StorageProperties와 동일 컨벤션).
 *
 * 로컬 기본값은 전부 비활성(client-id 없음)이며, 배포 환경마다 env로 오버라이드한다.
 * mock은 외부 호출 없이 전체 플로우를 검증하기 위한 QA E2E 전용으로 SSO_MOCK_ENABLED로만 켠다.
 */
@ConfigurationProperties(prefix = "teamsync.sso")
public record SsoProperties(
		Google google,
		Microsoft microsoft,
		Mock mock
) {

	public record Google(String clientId, String clientSecret) {
	}

	public record Microsoft(String clientId, String clientSecret, String tenant) {
	}

	public record Mock(boolean enabled, String email) {
	}
}
