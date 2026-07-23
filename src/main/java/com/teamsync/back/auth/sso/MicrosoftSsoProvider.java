package com.teamsync.back.auth.sso;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/** FR-002 SSO: Microsoft Entra ID(Azure AD) OpenID Connect 공급자. */
@Component
public class MicrosoftSsoProvider extends AbstractOAuth2SsoProvider {

	private final SsoProperties.Microsoft config;

	public MicrosoftSsoProvider(SsoProperties properties, RestClient.Builder restClientBuilder) {
		super(restClientBuilder.build());
		this.config = properties.microsoft();
	}

	@Override
	public String name() {
		return "microsoft";
	}

	private String tenant() {
		String tenant = config == null ? null : config.tenant();
		return StringUtils.hasText(tenant) ? tenant : "common";
	}

	@Override
	protected String authorizationEndpoint() {
		return "https://login.microsoftonline.com/" + tenant() + "/oauth2/v2.0/authorize";
	}

	@Override
	protected String tokenEndpoint() {
		return "https://login.microsoftonline.com/" + tenant() + "/oauth2/v2.0/token";
	}

	@Override
	protected String userInfoEndpoint() {
		return "https://graph.microsoft.com/oidc/userinfo";
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
