-- FR-002: 역할 체계 4단계(ADMIN/LEADER/MEMBER/GUEST) -> 5단계
-- (ADMIN/LEADER/MANAGER/ASSISTANT_MANAGER/STAFF/GUEST) 변경.
-- MANAGER(과장)/ASSISTANT_MANAGER(대리)/STAFF(사원)는 기존 단일 MEMBER 역할을 세분화한
-- 것으로, 권한상 항상 동일하게 취급된다(PRD 4장 권한 매트릭스). 세부 직급 정보가 없는
-- 기존 MEMBER 데이터는 안전한 기본값으로 최하위 신규 티어인 STAFF(사원)로 매핑한다.

ALTER TABLE users DROP CONSTRAINT chk_users_role;

UPDATE users SET role = 'STAFF' WHERE role = 'MEMBER';

ALTER TABLE users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('ADMIN', 'LEADER', 'MANAGER', 'ASSISTANT_MANAGER', 'STAFF', 'GUEST'));

ALTER TABLE invitations DROP CONSTRAINT chk_invitations_role;

UPDATE invitations SET role = 'STAFF' WHERE role = 'MEMBER';

ALTER TABLE invitations
    ADD CONSTRAINT chk_invitations_role
        CHECK (role IN ('LEADER', 'MANAGER', 'ASSISTANT_MANAGER', 'STAFF', 'GUEST'));
