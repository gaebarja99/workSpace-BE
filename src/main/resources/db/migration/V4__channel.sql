-- FR-201 채널(=토픽), FR-202 실시간 메시징(이번 마이그레이션은 REST + 프론트 폴링으로 근사, WebSocket
-- 실시간 전송 채널은 후속 과제). 태스크-메시지 연동(FR-301~305)에 필요한 task_message_links,
-- 채널 멤버십/PRIVATE 접근 제어 세부 테이블은 이번 범위 밖이며 후속 마이그레이션에서 확장한다.

CREATE TABLE channels (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT NOT NULL REFERENCES projects (id),
    name         VARCHAR(100) NOT NULL,
    visibility   VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    created_by   BIGINT REFERENCES users (id),
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_channels_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE'))
);

CREATE INDEX idx_channels_project_id ON channels (project_id);

-- FR-202: 최상위 메시지와 스레드 답글(parent_message_id)을 하나의 테이블로 관리한다.
CREATE TABLE messages (
    id                BIGSERIAL PRIMARY KEY,
    channel_id        BIGINT NOT NULL REFERENCES channels (id),
    author_id         BIGINT NOT NULL REFERENCES users (id),
    content           TEXT NOT NULL,
    parent_message_id BIGINT REFERENCES messages (id),
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_channel_id ON messages (channel_id);
CREATE INDEX idx_messages_parent_message_id ON messages (parent_message_id);
