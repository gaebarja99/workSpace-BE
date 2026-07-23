package com.teamsync.back.auth.sso.dto;

/** FR-002 SSO: 공급자 목록 항목. enabled면 FE에서 해당 SSO 버튼을 활성화한다. */
public record SsoProviderInfo(String provider, boolean enabled) {
}
