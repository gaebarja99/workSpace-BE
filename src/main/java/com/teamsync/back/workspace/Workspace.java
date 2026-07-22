package com.teamsync.back.workspace;

import com.teamsync.back.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-000: 회사 단위 워크스페이스.
 * domain(이메일 도메인)을 기준으로 신규 가입자가 자동으로 합류할 워크스페이스를 판별한다.
 * 도메인 소유권(DNS TXT 등) 검증은 이번 단계 범위 밖(PRD 5.6 리스크, 향후 과제)이다.
 *
 * 향후 이 엔티티 하위로 Board/Column/Task, Channel/Message, ArchiveItem이 Project를 거쳐
 * 연결될 예정이다(PRD 5.3 계층: Workspace -> Project -> Board/Channel/Archive).
 */
@Getter
@Entity
@Table(name = "workspaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workspace extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, unique = true, length = 255)
	private String domain;

	public Workspace(String name, String domain) {
		this.name = name;
		this.domain = domain;
	}
}
