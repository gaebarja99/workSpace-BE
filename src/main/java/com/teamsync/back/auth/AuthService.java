package com.teamsync.back.auth;

import com.teamsync.back.auth.dto.LoginRequest;
import com.teamsync.back.auth.dto.SignupRequest;
import com.teamsync.back.auth.dto.TokenResponse;
import com.teamsync.back.auth.dto.UserSummary;
import com.teamsync.back.common.exception.DuplicateEmailException;
import com.teamsync.back.common.exception.InvalidCredentialsException;
import com.teamsync.back.common.exception.WorkspaceNameRequiredException;
import com.teamsync.back.user.Role;
import com.teamsync.back.user.User;
import com.teamsync.back.user.UserRepository;
import com.teamsync.back.workspace.Workspace;
import com.teamsync.back.workspace.WorkspaceRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-000(워크스페이스 생성/도메인 기반 가입) + FR-002(이메일 로그인, JWT 발급).
 */
@Service
public class AuthService {

	private final WorkspaceRepository workspaceRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthService(WorkspaceRepository workspaceRepository, UserRepository userRepository,
			PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
		this.workspaceRepository = workspaceRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Transactional
	public TokenResponse signup(SignupRequest request) {
		String email = request.email().trim().toLowerCase();

		if (userRepository.existsByEmail(email)) {
			throw new DuplicateEmailException(email);
		}

		String domain = extractDomain(email);
		Workspace workspace = workspaceRepository.findByDomain(domain).orElse(null);

		Role role;
		if (workspace != null) {
			// FR-000: 도메인이 일치하는 워크스페이스가 이미 있으면 자동 합류(멤버로)
			role = Role.MEMBER;
		} else {
			// 새 워크스페이스 생성: 최초 생성자는 관리자(ADMIN)가 된다.
			if (request.workspaceName() == null || request.workspaceName().isBlank()) {
				throw new WorkspaceNameRequiredException(domain);
			}
			workspace = workspaceRepository.save(new Workspace(request.workspaceName().trim(), domain));
			role = Role.ADMIN;
		}

		User user = userRepository.save(
				new User(workspace, email, passwordEncoder.encode(request.password()), request.name().trim(), role));

		return issueToken(user);
	}

	@Transactional(readOnly = true)
	public UserSummary getCurrentUser(AuthenticatedUser principal) {
		User user = userRepository.findById(principal.userId())
				.orElseThrow(InvalidCredentialsException::new);
		return UserSummary.from(user);
	}

	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest request) {
		String email = request.email().trim().toLowerCase();
		User user = userRepository.findByEmail(email)
				.orElseThrow(InvalidCredentialsException::new);

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}

		return issueToken(user);
	}

	private TokenResponse issueToken(User user) {
		String accessToken = jwtTokenProvider.generateAccessToken(user);
		return TokenResponse.of(accessToken, jwtTokenProvider.getAccessTokenExpirationMs(), UserSummary.from(user));
	}

	private String extractDomain(String email) {
		int at = email.indexOf('@');
		return email.substring(at + 1);
	}
}
