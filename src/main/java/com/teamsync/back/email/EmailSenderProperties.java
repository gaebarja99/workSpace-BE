package com.teamsync.back.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 이메일 발송기 설정. BackApplication의 @ConfigurationPropertiesScan으로 자동 등록된다
 * (InvitationProperties와 동일 컨벤션).
 *
 * 배포 env에 자격증명이 없는 경우를 기본으로 가정해 기본 비활성(로그 Mock)이며,
 * env(EMAIL_ENABLED 등)로 실발송을 켠다.
 */
@ConfigurationProperties(prefix = "email")
public record EmailSenderProperties(boolean enabled, String from) {
}
