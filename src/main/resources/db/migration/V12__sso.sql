-- FR-002 SSO(Google/Microsoft/Mock) 로그인.
-- SSO 유저는 비밀번호가 없으므로 password_hash를 nullable로 완화하고, 계정의 인증 출처를
-- auth_provider로 구분한다. ddl-auto=validate이므로 이 스키마가 User 엔티티와 정확히 일치해야 한다.

-- 1) SSO 유저(JIT 프로비저닝)는 password_hash가 없다 -> NOT NULL 제약 해제.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- 2) 계정 인증 출처. 기존 이메일 가입 유저는 전부 LOCAL로 채운다.
--    LOCAL(이메일+비밀번호) / GOOGLE / MICROSOFT / MOCK(QA E2E 용).
ALTER TABLE users ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD CONSTRAINT chk_users_auth_provider
    CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'MICROSOFT', 'MOCK'));
