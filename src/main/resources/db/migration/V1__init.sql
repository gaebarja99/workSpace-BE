-- FR-000 워크스페이스, FR-002 인증/권한, FR-001 프로젝트(최소 골격)
-- 향후 Board/Column/Task, Channel/Message, ArchiveItem은 projects.id를 참조하는
-- 별도 마이그레이션(V2 이후)으로 확장한다 (PRD 5.3 계층 구조: Workspace -> Project -> Board/Channel/Archive).

CREATE TABLE workspaces (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    domain      VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_workspaces_domain UNIQUE (domain)
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    workspace_id  BIGINT NOT NULL REFERENCES workspaces (id),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    role          VARCHAR(20) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'LEADER', 'MEMBER', 'GUEST'))
);

CREATE INDEX idx_users_workspace_id ON users (workspace_id);

CREATE TABLE projects (
    id           BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces (id),
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    created_by   BIGINT REFERENCES users (id),
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_projects_workspace_id ON projects (workspace_id);
