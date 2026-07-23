package com.teamsync.back.auth.sso;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** FR-002 SSO: Google OAuth2/OpenID Connect 공급자. */
@Component
public class GoogleSsoProvider extends AbstractOAuth2SsoProvider {

	private final SsoProperties.Google config;

	public GoogleSsoProvider(SsoProperties properties, RestClient.Builder restClientBuilder) {
		super(restClientBuilder.build());
		this.config = properties.google();
	}

	@Override
	public String name() {
		return "google";
	}

	@Override
	protected String authorizationEndpoint() {
		return "https://accounts.google.com/o/oauth2/v2/auth";
	}

	@Override
	protected String tokenEndpoint() {
		return "https://oauth2.googleapis.com/token";
	}

	@Override
	protected String userInfoEndpoint() {
		return "https://openidconnect.googleapis.com/v1/userinfo";
	}

	@Override
	protected String clientId() {
		return config == null ? null : config.clientId();
	}

	@Override
	protected String clientSecret() {
		return config == null ? null : config.clientSecret();
	}
}
