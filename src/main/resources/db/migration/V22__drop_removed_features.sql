-- 팀 채팅(채널/메시지)/이슈 자동 플래그/파일함·위키 아카이브/알림/다이렉트 메시지 기능을 전면
-- 제거하면서 각 기능 전용 테이블/컬럼을 정리한다. 히스토리 보존을 위해 기존 V*.sql은 수정하지 않고
-- 이 마이그레이션에서 CASCADE로 관련 테이블/컬럼을 한 번에 drop한다(PostgreSQL 문법).
--
-- 남겨두는 것: task_comments/task_comment_mentions/task_activities(FR-105 태스크 댓글·활동 로그는
-- 채팅과 무관한 태스크 도메인 기능이라 유지), report_template_sections.section_key CHECK 제약(V20,
-- 'HIGHLIGHTS' 값은 애플리케이션에서 더 이상 생성하지 않지만 과거 이력 보존을 위해 제약 자체는 손대지 않음).

-- ----- 팀 채팅(FR-201/202/203, 태스크-메시지 연동 FR-301~305) -----
DROP TABLE IF EXISTS task_message_links CASCADE;
DROP TABLE IF EXISTS message_mentions CASCADE;
DROP TABLE IF EXISTS message_reactions CASCADE;
DROP TABLE IF EXISTS messages CASCADE;
DROP TABLE IF EXISTS channels CASCADE;

-- ----- 파일함 위키 아카이브(FR-204/205, 태스크-파일 연동 FR-304) -----
DROP TABLE IF EXISTS task_file_links CASCADE;
DROP TABLE IF EXISTS archived_file_tags CASCADE;
DROP TABLE IF EXISTS archived_files CASCADE;
DROP TABLE IF EXISTS archive_item_tags CASCADE;
DROP TABLE IF EXISTS archive_items CASCADE;

-- ----- 알림(FR-003/108/408) -----
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS notification_preferences CASCADE;

-- ----- 다이렉트 메시지(FR-206) -----
DROP TABLE IF EXISTS dm_messages CASCADE;
DROP TABLE IF EXISTS dm_conversation_participants CASCADE;
DROP TABLE IF EXISTS dm_conversations CASCADE;

-- ----- 이슈/리스크 자동 플래그(FR-406) -----
DROP TABLE IF EXISTS task_issue_flags CASCADE;

-- ----- tasks 컬럼 정리 -----
-- channel_notifications_enabled(V8): FR-302 채널 시스템 메시지 on/off 토글, 채팅 제거로 의미 없어짐.
ALTER TABLE tasks DROP COLUMN IF EXISTS channel_notifications_enabled;
