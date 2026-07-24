package com.teamsync.back.notification;

/**
 * FR-003 발송 훅에서 사용하는, 특정 (사용자 x 카테고리)에 대해 최종 확정된 채널 on/off 조합.
 * 저장된 설정이 있으면 그 값을, 없으면 카테고리 기본값을 담는다.
 */
public record EffectiveChannels(boolean inApp, boolean email, boolean push) {
}
