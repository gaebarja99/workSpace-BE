package com.teamsync.back.task.issue;

/**
 * FR-406: 이슈 플래그의 처리 상태. OPEN은 태스크+kind 조합당 최대 1건만 존재할 수 있고
 * (V18 부분 유니크 인덱스), RESOLVED는 자동(배치 재계산으로 조건 해소) 또는 수동
 * (PATCH .../issues/{issueId}/resolve)으로 여러 건 쌓일 수 있다.
 */
public enum TaskIssueStatus {
	OPEN,
	RESOLVED
}
