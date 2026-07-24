package com.teamsync.back.project;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.user.User;
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

/**
 * FR-001 프로젝트(=토픽) 최소 골격.
 * PRD 5.3 계층 구조(Workspace -> Project -> Board/Column/Task, Channel/Message, ArchiveItem) 중
 * 이번 단계는 Project 엔티티까지만 구현한다. Board/Column/Task, Channel/Message, ArchiveItem은
 * 모두 이 Project(project_id)를 참조하는 후속 마이그레이션으로 확장될 예정이며 이번 범위에서는 생성하지 않는다.
 */
@Getter
@Entity
@Table(name = "projects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	// 프로젝트 관리(관리자, P2): 진행 상태. 신규 프로젝트는 항상 ACTIVE로 시작하고,
	// 관리자만 PATCH /api/admin/projects/{id}/status로 전환할 수 있다.
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProjectStatus status = ProjectStatus.ACTIVE;

	public Project(Workspace workspace, String name, String description, User createdBy) {
		this.workspace = workspace;
		this.name = name;
		this.description = description;
		this.createdBy = createdBy;
	}

	/** 프로젝트 관리(관리자, P2): 관리자가 프로젝트 상태를 변경한다. */
	public void changeStatus(ProjectStatus status) {
		this.status = status;
	}
}
