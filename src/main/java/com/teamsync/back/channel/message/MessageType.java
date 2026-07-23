package com.teamsync.back.channel.message;

/**
 * FR-301~305(메시지↔태스크 연동, US-09/10): 메시지의 출처를 구분하기 위한 타입.
 * 프론트에서는 뱃지 표시용으로만 사용하며, 서버는 이 값에 따라 별도 분기 처리를 하지 않는다.
 */
public enum MessageType {
	/** 사람이 채널에서 직접 작성한 메시지(기존 전체 데이터가 여기에 해당). */
	USER,
	/** FR-302: 태스크 생성/상태변경/완료 시 서버가 자동 게시하는 메시지. author는 항상 null. */
	SYSTEM,
	/** FR-305: 태스크 댓글 작성 시 연결된 채널 스레드로 동기화된 메시지. author는 댓글 작성자 그대로. */
	TASK_COMMENT_SYNC
}
