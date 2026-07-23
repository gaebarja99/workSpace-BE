package com.teamsync.back.user;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.workspace.Workspace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	// FR-002 SSO: SSO(JIT) 유저는 비밀번호가 없으므로 nullable. LOCAL 유저만 값이 존재한다.
	@Column(name = "password_hash", length = 255)
	private String passwordHash;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	// FR-002: 계정 인증 출처(LOCAL/GOOGLE/MICROSOFT/MOCK).
	@Enumerated(EnumType.STRING)
	@Column(name = "auth_provider", nullable = false, length = 20)
	private AuthProvider authProvider;

	/** 이메일 가입(LOCAL): 비밀번호 해시를 갖는다. */
	public User(Workspace workspace, String email, String passwordHash, String name, Role role) {
		this.workspace = workspace;
		this.email = email;
		this.passwordHash = passwordHash;
		this.name = name;
		this.role = role;
		this.authProvider = AuthProvider.LOCAL;
	}

	/** FR-002 SSO(JIT 프로비저닝): 비밀번호 없이 생성한다. authProvider는 GOOGLE/MICROSOFT/MOCK. */
	public User(Workspace workspace, String email, String name, Role role, AuthProvider authProvider) {
		this.workspace = workspace;
		this.email = email;
		this.passwordHash = null;
		this.name = name;
		this.role = role;
		this.authProvider = authProvider;
	}
}
