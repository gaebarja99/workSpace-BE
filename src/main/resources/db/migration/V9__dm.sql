-- FR-206(1:1 및 소그룹 다이렉트 메시지). Channel.project는 NOT NULL이고 ChannelVisibility는
-- "프로젝트 내부 가시성" 개념이라 프로젝트에 속하지 않는 DM에는 억지로 재사용하지 않는다. 완전히 독립된
-- com.teamsync.back.dm 모듈 전용 테이블을 신설한다(계약 문서 fr206-contract.md 참고, FR-201/202/203/301-305가
-- 의존하는 channels/messages 테이블은 이 마이그레이션에서 전혀 건드리지 않는다).
--
-- 계약 문서 초안 SQL과의 차이: 기존 컨벤션(channels/messages 등 BaseTimeEntity 상속 엔티티는 모두
-- created_at/updated_at 두 컬럼을 함께 가짐)을 따르기 위해 dm_conversations/dm_messages 모두
-- updated_at 컬럼을 추가했다. dm_conversation_participants(user_id)에는 "호출자가 참가자인 대화 목록"
-- 조회(GET /api/dm/conversations)를 위한 보조 인덱스를 추가했다(PK가 (conversation_id, user_id)라
-- user_id 단독 조회는 PK만으로 커버되지 않음).

CREATE TABLE dm_conversations (
    id         BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE dm_conversation_participants (
    conversation_id BIGINT NOT NULL REFERENCES dm_conversations (id),
    user_id         BIGINT NOT NULL REFERENCES users (id),
    PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_dm_conversation_participants_user_id ON dm_conversation_participants (user_id);

-- DM에는 SYSTEM 메시지가 없으므로(FR-302 같은 자동 게시 대상이 아님) author_id는 항상 NOT NULL이다.
CREATE TABLE dm_messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES dm_conversations (id),
    author_id       BIGINT NOT NULL REFERENCES users (id),
    content         TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_dm_messages_conversation ON dm_messages (conversation_id, created_at, id);
