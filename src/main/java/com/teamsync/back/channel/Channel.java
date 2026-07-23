package com.teamsync.back.channel;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.project.Project;
import com.teamsync.back.user.User;
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
 * FR-201 채널(=토픽) 최소 골격.
 * Task/Project와 동일한 원칙으로 project(project_id)를 통해서만 워크스페이스에 스코핑되며,
 * 클라이언트가 workspaceId를 직접 지정하지 않는다(PRD 5.6 리스크 대응).
 * 태스크-메시지 연동(FR-301~305)에 필요한 task_message_links 등은 이번 범위 밖이며 후속 마이그레이션에서 다룬다.
 */
@Getter
@Entity
@Table(name = "channels")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ChannelVisibility visibility;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by")
	private User createdBy;

	public Channel(Project project, String name, ChannelVisibility visibility, User createdBy) {
		this.project = project;
		this.name = name;
		this.visibility = visibility;
		this.createdBy = createdBy;
	}
}
