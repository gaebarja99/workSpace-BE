package com.teamsync.back.auth.sso.dto;

/** FR-002 SSO: GET /api/auth/sso/{provider}/authorize 응답. FE가 이 URL로 브라우저를 redirect한다. */
public record SsoAuthorizeResponse(String authorizationUrl) {
}
