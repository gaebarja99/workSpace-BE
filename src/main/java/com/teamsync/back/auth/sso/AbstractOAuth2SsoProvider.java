package com.teamsync.back.auth.sso;

import com.teamsync.back.common.exception.SsoExchangeFailedException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * FR-002 SSO: 표준 OAuth2 Authorization Code 플로우를 공유하는 공급자 베이스
 * (Google/Microsoft). 각 공급자는 엔드포인트/자격증명/스코프만 구현하면 된다.
 * spring-boot-starter-web에 포함된 RestClient로 token/userinfo를 호출한다.
 */
abstract class AbstractOAuth2SsoProvider implements SsoProvider {

	protected final RestClient restClient;

	protected AbstractOAuth2SsoProvider(RestClient restClient) {
		this.restClient = restClient;
	}

	protected abstract String authorizationEndpoint();

	protected abstract String tokenEndpoint();

	protected abstract String userInfoEndpoint();

	protected abstract String clientId();

	protected abstract String clientSecret();

	protected String scope() {
		return "openid email profile";
	}

	@Override
	public boolean isEnabled() {
		return StringUtils.hasText(clientId()) && StringUtils.hasText(clientSecret());
	}

	@Override
	public String buildAuthorizationUrl(String redirectUri, String signedState) {
		return UriComponentsBuilder.fromUriString(authorizationEndpoint())
				.queryParam("client_id", clientId())
				.queryParam("redirect_uri", redirectUri)
				.queryParam("response_type", "code")
				.queryParam("scope", scope())
				.queryParam("state", signedState)
				.build()
				.encode()
				.toUriString();
	}

	@Override
	public SsoUserInfo exchange(String code, String redirectUri) {
		String accessToken = requestAccessToken(code, redirectUri);
		return fetchUserInfo(accessToken);
	}

	private String requestAccessToken(String code, String redirectUri) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("code", code);
		form.add("client_id", clientId());
		form.add("client_secret", clientSecret());
		form.add("redirect_uri", redirectUri);
		form.add("grant_type", "authorization_code");

		try {
			Map<String, Object> body = restClient.post()
					.uri(tokenEndpoint())
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.accept(MediaType.APPLICATION_JSON)
					.body(form)
					.retrieve()
					.body(MAP_TYPE);

			String accessToken = body == null ? null : asString(body.get("access_token"));
			if (!StringUtils.hasText(accessToken)) {
				throw new SsoExchangeFailedException("SSO 토큰 응답에 access_token이 없습니다.");
			}
			return accessToken;
		} catch (SsoExchangeFailedException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw new SsoExchangeFailedException("SSO 토큰 교환에 실패했습니다.");
		}
	}

	private SsoUserInfo fetchUserInfo(String accessToken) {
		try {
			Map<String, Object> info = restClient.get()
					.uri(userInfoEndpoint())
					.header("Authorization", "Bearer " + accessToken)
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.body(MAP_TYPE);

			if (info == null) {
				throw new SsoExchangeFailedException("SSO 사용자 정보 응답이 비어 있습니다.");
			}
			String email = extractEmail(info);
			if (!StringUtils.hasText(email)) {
				throw new SsoExchangeFailedException("SSO 사용자 정보에 이메일이 없습니다.");
			}
			String name = asString(info.get("name"));
			if (!StringUtils.hasText(name)) {
				name = email.substring(0, email.indexOf('@'));
			}
			return new SsoUserInfo(email, name);
		} catch (SsoExchangeFailedException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw new SsoExchangeFailedException("SSO 사용자 정보 조회에 실패했습니다.");
		}
	}

	/**
	 * email 클레임을 추출한다. 공급자별로 email이 없을 수 있어(예: MS는 preferred_username/upn에
	 * 담기기도 함) 대체 클레임을 순차 확인한다.
	 */
	protected String extractEmail(Map<String, Object> info) {
		String email = asString(info.get("email"));
		if (!StringUtils.hasText(email)) {
			email = asString(info.get("preferred_username"));
		}
		if (!StringUtils.hasText(email)) {
			email = asString(info.get("upn"));
		}
		return email;
	}

	protected static String asString(Object value) {
		return value == null ? null : value.toString();
	}

	private static final org.springframework.core.ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
			new org.springframework.core.ParameterizedTypeReference<>() {
			};
}
