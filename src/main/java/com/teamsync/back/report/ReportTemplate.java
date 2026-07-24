package com.teamsync.back.report;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.project.Project;
import com.teamsync.back.workspace.Workspace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * FR-405(P3, 보고서 템플릿 관리): 조직(워크스페이스)/팀(프로젝트)별 커스텀 주간보고 양식.
 * project=null이면 워크스페이스 전사 기본 템플릿(워크스페이스당 1개, V20 부분 유니크 인덱스),
 * project가 있으면 해당 프로젝트(팀) 전용 템플릿(프로젝트당 1개)이다.
 * 이 엔티티는 "양식(섹션 구성)"만 정의하며, 실제 주간보고 데이터(WeeklyReport)와는 아직 연동하지
 * 않는다 — WeeklyReportService의 실시간 4섹션 계산 로직은 이 템플릿과 무관하게 그대로 동작한다.
 */
@Getter
@Entity
@Table(name = "report_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportTemplate extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id")
	private Project project;

	@Column(nullable = false, length = 200)
	private String name;

	public ReportTemplate(Workspace workspace, Project project, String name) {
		this.workspace = workspace;
		this.project = project;
		this.name = name;
	}

	public void changeName(String name) {
		this.name = name;
	}
}
