package com.teamsync.back.auth.sso;

/**
 * FR-002 SSO: provider userinfo에서 추출한 최소 사용자 식별 정보.
 * JIT 프로비저닝(워크스페이스 매칭 + find-or-create)에 email/name만 사용한다.
 */
public record SsoUserInfo(String email, String name) {
}
