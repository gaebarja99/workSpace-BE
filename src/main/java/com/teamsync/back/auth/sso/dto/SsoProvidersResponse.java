package com.teamsync.back.auth.sso.dto;

import java.util.List;

/** FR-002 SSO: GET /api/auth/sso/providers 응답. */
public record SsoProvidersResponse(List<SsoProviderInfo> providers) {
}
