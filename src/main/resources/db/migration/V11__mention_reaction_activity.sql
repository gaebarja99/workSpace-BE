-- FR-105(태스크 댓글 @멘션 + 활동 로그) & FR-202(메시지 @멘션 + 이모지 반응).
-- V7/V10의 CHECK 재생성 및 컬럼 추가 패턴을 그대로 따른다. ddl-auto=validate이므로 이 스키마가
-- 엔티티(Notification/TaskComment/TaskActivity/Message/MessageReaction)와 정확히 일치해야 한다.

-- =====================================================================
-- 0. 공통: 멘션 알림 인프라
-- =====================================================================

-- 0-1. notifications.type CHECK(V10)에 MENTION 추가.
ALTER TABLE notifications DROP CONSTRAINT chk_notifications_type;
ALTER TABLE notifications ADD CONSTRAINT chk_notifications_type
    CHECK (type IN ('TASK_DUE_SOON', 'TASK_DUE_TODAY', 'TASK_ASSIGNED', 'TASK_STATUS_CHANGED',
                     'WEEKLY_REPORT_REMINDER', 'MENTION'));

-- 0-2. 메시지 멘션 알림 딥링크(channel_id + message_id). task_id(V7)와 동일하게 원본이 삭제되어도
-- 알림 이력은 보존해야 하므로 nullable + ON DELETE SET NULL로 구성한다.
ALTER TABLE notifications ADD COLUMN channel_id BIGINT REFERENCES channels (id) ON DELETE SET NULL;
ALTER TABLE notifications ADD COLUMN message_id BIGINT REFERENCES messages (id) ON DELETE SET NULL;

-- =====================================================================
-- 1. FR-105-A: 태스크 댓글 @멘션
-- =====================================================================

-- task_comments의 언급 대상(다대다). 댓글 삭제 시 함께 정리(ON DELETE CASCADE).
CREATE TABLE task_comment_mentions (
    task_comment_id BIGINT NOT NULL REFERENCES task_comments (id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES users (id),
    PRIMARY KEY (task_comment_id, user_id)
);

-- =====================================================================
-- 2. FR-105-B: 태스크 활동 로그
-- =====================================================================

-- actor_id는 시스템/자동 발생 활동을 위해 nullable. activity_type은 CHECK로 제약.
CREATE TABLE task_activities (
    id            BIGSERIAL PRIMARY KEY,
    task_id       BIGINT NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    actor_id      BIGINT REFERENCES users (id),
    activity_type VARCHAR(30) NOT NULL,
    detail        TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_task_activities_type
        CHECK (activity_type IN ('CREATED', 'STATUS_CHANGED', 'ASSIGNEE_CHANGED', 'COMMENT_ADDED'))
);

-- GET /api/tasks/{taskId}/activities: task_id 기준 createdAt 오름차순 조회 최적화.
CREATE INDEX idx_task_activities_task_id_created_at ON task_activities (task_id, created_at);

-- =====================================================================
-- 3. FR-202-A: 메시지 @멘션
-- =====================================================================

-- messages에 @전체 여부 플래그 추가(V10 highlighted 추가와 동일 형태).
ALTER TABLE messages ADD COLUMN mention_everyone BOOLEAN NOT NULL DEFAULT FALSE;

-- 메시지별 개별 멘션 대상(다대다). 메시지 삭제 시 함께 정리(ON DELETE CASCADE).
CREATE TABLE message_mentions (
    message_id BIGINT NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users (id),
    PRIMARY KEY (message_id, user_id)
);

-- =====================================================================
-- 4. FR-202-B: 메시지 이모지 반응
-- =====================================================================

-- (message_id, user_id, emoji) 유일. 한 유저가 같은 메시지에 같은 이모지를 두 번 남길 수 없다(토글).
CREATE TABLE message_reactions (
    id         BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users (id),
    emoji      VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_message_reactions_message_user_emoji UNIQUE (message_id, user_id, emoji)
);

-- MessageResponse.reactions 집계(메시지별 전체 반응 조회) 최적화.
CREATE INDEX idx_message_reactions_message_id ON message_reactions (message_id);
