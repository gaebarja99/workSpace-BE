-- 구성원 관리/초대·승인(P1): users.status(활성/비활성) + invitations(토큰 기반 초대) 테이블.
-- ddl-auto=validate이므로 User/Invitation 엔티티와 정확히 일치해야 한다.

ALTER TABLE users
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE users
    ADD CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'DEACTIVATED'));

CREATE TABLE invitations (
    id                 BIGSERIAL PRIMARY KEY,
    workspace_id       BIGINT NOT NULL REFERENCES workspaces (id),
    email              VARCHAR(255) NOT NULL,
    role               VARCHAR(20) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    token              VARCHAR(255) NOT NULL,
    invited_by_user_id BIGINT NOT NULL REFERENCES users (id),
    expires_at         TIMESTAMP NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_invitations_token UNIQUE (token),
    -- ADMIN은 초대 대상 역할로 허용하지 않는다(애플리케이션 레벨에서도 재검증).
    CONSTRAINT chk_invitations_role CHECK (role IN ('LEADER', 'MEMBER', 'GUEST')),
    CONSTRAINT chk_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED'))
);

-- "이미 PENDING인 동일 (workspace_id, email) 초대 존재" 체크는 애플리케이션 레벨에서 조회 후 판단한다
-- (DB 조건부 유니크 인덱스는 과설계로 보고 강제하지 않는다).
CREATE INDEX idx_invitations_workspace_email_status ON invitations (workspace_id, email, status);
