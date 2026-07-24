-- FR-106(반복 태스크 템플릿, 주간/월간 자동 생성). ddl-auto=validate이므로 이 스키마가
-- RecurringTaskTemplate/Task 엔티티와 정확히 일치해야 한다.
-- 원칙: recurring_task_templates는 "생성 규칙"만 보관하고, 실제 Task는 RecurringTaskSchedulerService의
-- 일 배치가 그때그때 새로 만든다(tasks.recurring_template_id로 유래를 역추적).

CREATE TABLE recurring_task_templates (
    id                 BIGSERIAL PRIMARY KEY,
    project_id         BIGINT NOT NULL REFERENCES projects (id),
    title              VARCHAR(200) NOT NULL,
    description        TEXT,
    priority           VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    recurrence_type    VARCHAR(20) NOT NULL,
    day_of_week        VARCHAR(20),
    day_of_month       INT,
    due_in_days        INT NOT NULL,
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_by         BIGINT REFERENCES users (id),
    last_generated_at  DATE,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_recurring_task_templates_priority CHECK (priority IN ('URGENT', 'HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_recurring_task_templates_recurrence_type CHECK (recurrence_type IN ('WEEKLY', 'MONTHLY')),
    CONSTRAINT chk_recurring_task_templates_day_of_week
        CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT chk_recurring_task_templates_day_of_month CHECK (day_of_month BETWEEN 1 AND 31),
    CONSTRAINT chk_recurring_task_templates_due_in_days CHECK (due_in_days >= 0)
);

CREATE INDEX idx_recurring_task_templates_project_id ON recurring_task_templates (project_id);

-- 배치(RecurringTaskSchedulerService)가 매일 active=true 템플릿 전체를 조회하는 데 사용.
CREATE INDEX idx_recurring_task_templates_active ON recurring_task_templates (active);

-- 템플릿당 담당자 1명 이상(애플리케이션 레벨 검증), 다대다 연결. task_assignees와 동일 패턴.
CREATE TABLE recurring_task_template_assignees (
    template_id BIGINT NOT NULL REFERENCES recurring_task_templates (id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users (id),
    PRIMARY KEY (template_id, user_id)
);

-- FR-106: 이 태스크가 어떤 반복 템플릿에서 자동 생성됐는지 추적한다(일반 생성/메시지 전환 태스크는 NULL).
-- ON DELETE SET NULL: 템플릿을 하드 삭제해도 이미 생성된 태스크는 그대로 유지되고 이 참조만 사라진다.
ALTER TABLE tasks ADD COLUMN recurring_template_id BIGINT REFERENCES recurring_task_templates (id) ON DELETE SET NULL;

CREATE INDEX idx_tasks_recurring_template_id ON tasks (recurring_template_id);
