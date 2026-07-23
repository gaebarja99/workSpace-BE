-- FR-203 메시지 고정(US-07): 팀장/관리자가 공지성 메시지를 채널 상단에 고정할 수 있도록
-- messages 테이블에 고정 여부/고정 시각 컬럼을 추가한다.

ALTER TABLE messages ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE messages ADD COLUMN pinned_at TIMESTAMP;
