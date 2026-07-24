package com.teamsync.back.invitation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 구성원 관리(P1): 초대 이메일에 담을 프론트엔드 링크 베이스 URL.
 * BackApplication의 @ConfigurationPropertiesScan으로 자동 등록된다(SsoProperties/StorageProperties와 동일 컨벤션).
 * 링크 형식: {frontendBaseUrl}/invite/{token}
 */
@ConfigurationProperties(prefix = "app")
public record InvitationProperties(
		String frontendBaseUrl
) {

	public InvitationProperties {
		if (frontendBaseUrl == null || frontendBaseUrl.isBlank()) {
			frontendBaseUrl = "http://localhost:3000";
		}
	}
}
