package com.teamsync.back.dm;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-206(1:1 및 소그룹 다이렉트 메시지). 프로젝트에 종속되지 않는 최상위 대화이므로 Channel을 재사용하지 않고
 * 완전히 별도의 엔티티로 둔다(계약 문서 fr206-contract.md 참고).
 * participants는 Task.assignees와 동일한 패턴으로 User를 단방향 ManyToMany로 참조한다(중간 테이블
 * dm_conversation_participants는 순수 조인 테이블이며 별도 엔티티로 승격하지 않는다).
 * 그룹 대화의 이름(title) 필드는 두지 않는다 — 프론트엔드가 참가자 이름을 조합해 표시한다(1:1은 상대방
 * 이름, 그룹은 이름 나열). 워크스페이스 격리는 참가자(User)가 이미 워크스페이스에 속하고, 대화 생성 시
 * 모든 participantIds를 호출자와 같은 워크스페이스로 검증하므로(DmService 참고) 이 엔티티 자체에는
 * 별도의 workspace 참조 컬럼을 두지 않는다.
 */
@Getter
@Entity
@Table(name = "dm_conversations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DmConversation extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "dm_conversation_participants",
			joinColumns = @JoinColumn(name = "conversation_id"),
			inverseJoinColumns = @JoinColumn(name = "user_id"))
	private Set<User> participants = new LinkedHashSet<>();

	public DmConversation(Set<User> participants) {
		this.participants = new LinkedHashSet<>(participants);
	}
}
