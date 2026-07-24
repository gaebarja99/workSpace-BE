-- FR-107(P2, 간트뷰/타임라인 시각화는 Phase 2 이후로 축소): 태스크 간 선후행 의존관계.
-- 시각화 없이 데이터 모델+CRUD API+순환 감지까지만 다루며, 이 관계로 태스크 상태 전환을
-- 막는 등의 비즈니스 규칙은 이번 범위에 포함되지 않는다.

CREATE TABLE task_dependencies (
    id                   BIGSERIAL PRIMARY KEY,
    predecessor_task_id  BIGINT NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    successor_task_id    BIGINT NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    created_by           BIGINT REFERENCES users (id),
    created_at           TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_task_dependencies_predecessor_successor UNIQUE (predecessor_task_id, successor_task_id),
    CONSTRAINT chk_task_dependencies_no_self_reference CHECK (predecessor_task_id <> successor_task_id)
);

CREATE INDEX idx_task_dependencies_predecessor_task_id ON task_dependencies (predecessor_task_id);
CREATE INDEX idx_task_dependencies_successor_task_id ON task_dependencies (successor_task_id);
