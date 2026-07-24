-- FR-406(이슈/리스크 자동 플래그, 완전판): FR-401 주간보고 생성 시점의 일회성 OVERDUE/STALE
-- 스냅샷과 달리, 영속적인 플래그 엔티티 + 실시간 조회 API + 수동 해결(resolve)을 갖춘다.
-- kind: OVERDUE(마감 초과) | STALE(21일 이상 미변경) | BLOCKED(선행 태스크 미완료, 신규).
-- status: OPEN | RESOLVED. 동일 태스크+kind에 대해 OPEN은 최대 1건만 존재해야 하므로
-- (RESOLVED 이력은 여러 건 쌓여도 된다) 부분 유니크 인덱스로 강제한다.

CREATE TABLE task_issue_flags (
    id             BIGSERIAL PRIMARY KEY,
    task_id        BIGINT NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    kind           VARCHAR(20) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    detail         VARCHAR(500) NOT NULL,
    detected_at    TIMESTAMP NOT NULL,
    resolved_at    TIMESTAMP,
    resolved_by_id BIGINT REFERENCES users (id) ON DELETE SET NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_issue_flags_task_id ON task_issue_flags (task_id);

-- 동일 태스크+kind에 대해 OPEN 상태는 최대 1건만 존재하도록 강제(RESOLVED 이력은 여러 건 허용).
CREATE UNIQUE INDEX uk_task_issue_flags_task_kind_open ON task_issue_flags (task_id, kind) WHERE status = 'OPEN';
