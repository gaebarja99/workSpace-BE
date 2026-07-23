-- FR-301~305 메시지↔태스크 연동(US-09/10).
-- 트랜잭션 원칙(PRD 5.6): "메시지는 유지, 태스크는 참조만 생성" — task_message_links는 messages를
-- 참조만 할 뿐 messages 테이블 자체는 어떤 컬럼도 변경하지 않는다(FR-301 변환 실패 시 원본 메시지 불변 보장).

-- FR-302: 시스템 메시지(태스크 생성/상태변경/완료 자동 게시)는 사람이 작성하지 않으므로 author_id가 없다.
ALTER TABLE messages ALTER COLUMN author_id DROP NOT NULL;

-- USER(기존 전체 데이터) / SYSTEM(FR-302, author_id NULL) / TASK_COMMENT_SYNC(FR-305, author_id는 댓글 작성자)
ALTER TABLE messages ADD COLUMN message_type VARCHAR(20) NOT NULL DEFAULT 'USER';
ALTER TABLE messages ADD CONSTRAINT chk_messages_message_type
    CHECK (message_type IN ('USER', 'SYSTEM', 'TASK_COMMENT_SYNC'));

-- FR-302: 태스크별 채널 자동 알림 on/off 토글(PATCH /api/tasks/{taskId}로 변경 가능).
ALTER TABLE tasks ADD COLUMN channel_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE;

-- FR-301/303: 메시지 -> 태스크 변환 시 생성되는 단방향 참조. 태스크당 최대 1개(변환으로 생성된 태스크만
-- 이 링크를 가지며, 일반 생성 태스크는 링크가 없다). "관련 대화 보기"(FR-303)는 이 테이블을 통해
-- TaskResponse.linkedChannelId/linkedMessageId로 노출되며 별도 조회 엔드포인트를 두지 않는다.
CREATE TABLE task_message_links (
    id         BIGSERIAL PRIMARY KEY,
    task_id    BIGINT NOT NULL REFERENCES tasks (id),
    message_id BIGINT NOT NULL REFERENCES messages (id),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_task_message_links_task_id ON task_message_links (task_id);
CREATE INDEX idx_task_message_links_message_id ON task_message_links (message_id);

-- FR-304: 채널/아카이브에 업로드된 파일을 재업로드 없이 태스크 카드에 "링크"만 한다(다대다).
CREATE TABLE task_file_links (
    id               BIGSERIAL PRIMARY KEY,
    task_id          BIGINT NOT NULL REFERENCES tasks (id),
    archived_file_id BIGINT NOT NULL REFERENCES archived_files (id),
    linked_by        BIGINT NOT NULL REFERENCES users (id),
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_task_file_links_task_id_archived_file_id UNIQUE (task_id, archived_file_id)
);

CREATE INDEX idx_task_file_links_task_id ON task_file_links (task_id);

-- FR-305: 태스크 댓글. 작성 시(같은 트랜잭션) task_message_links가 있으면 해당 채널 스레드로
-- TASK_COMMENT_SYNC 메시지가 함께 게시된다.
CREATE TABLE task_comments (
    id         BIGSERIAL PRIMARY KEY,
    task_id    BIGINT NOT NULL REFERENCES tasks (id),
    author_id  BIGINT NOT NULL REFERENCES users (id),
    content    TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_comments_task_id ON task_comments (task_id);
