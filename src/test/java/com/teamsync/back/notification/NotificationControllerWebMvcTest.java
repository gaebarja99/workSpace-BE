package com.teamsync.back.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teamsync.back.auth.JwtProperties;
import com.teamsync.back.auth.JwtTokenProvider;
import com.teamsync.back.auth.RestAccessDeniedHandler;
import com.teamsync.back.auth.RestAuthenticationEntryPoint;
import com.teamsync.back.common.exception.InvalidNotificationPreferenceException;
import com.teamsync.back.config.SecurityConfig;
import com.teamsync.back.notification.dto.NotificationPreferenceView;
import com.teamsync.back.notification.dto.NotificationPreferencesResponse;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.workspace.Workspace;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * FR-003 /api/notifications/preferences HTTP 계층(보안 필터 + 라우팅 + 예외 매핑) 회귀 테스트.
 * 서비스 로직은 {@link NotificationPreferenceServiceTest}에서 검증하므로 여기서는 서비스를 목으로 대체하고,
 * 인증(401)·정상 응답 shape·검증 실패(400) 매핑만 확인한다(DB 불필요).
 */
@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, RestAuthenticationEntryPoint.class,
		RestAccessDeniedHandler.class, NotificationControllerWebMvcTest.TestBeans.class})
@TestPropertySource(properties = {
		"teamsync.cors.allowed-origins=http://localhost:3000",
		"teamsync.jwt.secret=test-only-secret-key-must-be-at-least-32-bytes-long!!",
		"teamsync.jwt.access-token-expiration-ms=60000"
})
class NotificationControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private NotificationService notificationService;

	@MockitoBean
	private NotificationPreferenceService preferenceService;

	// 메인 앱(@EnableJpaAuditing)이 요구하는 JPA 메타모델을 웹 슬라이스에는 두지 않으므로 목으로 대체한다.
	@MockitoBean
	private JpaMetamodelMappingContext jpaMetamodelMappingContext;

	// JwtAuthenticationFilter 회귀 방지(구성원 관리 P1): 인증 성공 후 PK 단건 조회로 계정을 다시 읽어
	// 상태(ACTIVE)/역할을 최신화하므로, 이 웹 슬라이스 테스트에서도 token()이 발급한 사용자와 동일한
	// id로 findById가 그 사용자(ACTIVE)를 반환하도록 목으로 대체한다.
	@MockitoBean
	private UserRepository userRepository;

	private User activeUser;

	@BeforeEach
	void stubActiveUser() throws Exception {
		Workspace workspace = new Workspace("그로우테크", "growtech.io");
		setId(workspace, 10L);
		activeUser = new User(workspace, "member@growtech.io", "hashed", "박멤버", Role.MEMBER);
		setId(activeUser, 1L);
		when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(activeUser));
	}

	@TestConfiguration
	static class TestBeans {
		@Bean
		JwtProperties jwtProperties(@Value("${teamsync.jwt.secret}") String secret,
				@Value("${teamsync.jwt.access-token-expiration-ms}") long ttl) {
			return new JwtProperties(secret, ttl);
		}
	}

	@Test
	void 인증없이_GET_preferences는_401() throws Exception {
		mockMvc.perform(get("/api/notifications/preferences"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 인증된_GET_preferences는_200과_6개_카테고리를_반환한다() throws Exception {
		when(preferenceService.getMyPreferences(any())).thenReturn(defaultResponse());

		mockMvc.perform(get("/api/notifications/preferences").header("Authorization", "Bearer " + token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.categories.length()").value(NotificationCategory.values().length))
				.andExpect(jsonPath("$.categories[0].category").value("TASK_DEADLINE"))
				.andExpect(jsonPath("$.categories[1].category").value("MENTION"))
				.andExpect(jsonPath("$.categories[1].push").value(true));
	}

	@Test
	void PUT_알수없는_category는_400_VALIDATION_ERROR() throws Exception {
		when(preferenceService.updateMyPreferences(any(), any()))
				.thenThrow(new InvalidNotificationPreferenceException("알 수 없는 category입니다: FOO"));

		mockMvc.perform(put("/api/notifications/preferences")
						.header("Authorization", "Bearer " + token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categories\":[{\"category\":\"FOO\"}]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void PUT_categories가_배열이_아니면_400() throws Exception {
		mockMvc.perform(put("/api/notifications/preferences")
						.header("Authorization", "Bearer " + token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categories\":\"not-an-array\"}"))
				.andExpect(status().isBadRequest());
	}

	private String token() throws Exception {
		return jwtTokenProvider.generateAccessToken(activeUser);
	}

	private static NotificationPreferencesResponse defaultResponse() {
		List<NotificationPreferenceView> views = java.util.Arrays.stream(NotificationCategory.values())
				.map(category -> NotificationPreferenceView.of(category, category.defaults()))
				.toList();
		return new NotificationPreferencesResponse(views);
	}

	private static void setId(Object entity, Long id) throws Exception {
		Field idField = entity.getClass().getDeclaredField("id");
		idField.setAccessible(true);
		idField.set(entity, id);
	}
}
