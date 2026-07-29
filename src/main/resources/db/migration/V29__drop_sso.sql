-- SSO(Google/Microsoft/Mock) 로그인 기능을 제품 결정으로 완전히 제거한다. 히스토리 보존을 위해
-- 기존 V12__sso.sql은 수정하지 않고, 이 마이그레이션에서 SSO로 완화했던 제약을 되돌린다.

-- 1) SSO(JIT 프로비저닝)로 생성된 계정은 비밀번호가 없어 로그인 수단이 사라진다. 개발 단계이므로
--    해당 계정을 정리한다(운영 환경에 실제 SSO 가입자가 있었다면 이 삭제 대신 별도 이관 절차가 필요).
DELETE FROM users WHERE auth_provider <> 'LOCAL';

-- 2) 이제 모든 유저는 LOCAL(이메일+비밀번호)만 존재하므로 password_hash를 다시 필수로 되돌린다(V12에서 완화).
ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;

-- 3) auth_provider 값 도메인을 LOCAL만 허용하도록 축소한다(V12의 CHECK 제약 교체).
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_auth_provider;
ALTER TABLE users ADD CONSTRAINT chk_users_auth_provider CHECK (auth_provider = 'LOCAL');
