package com.teamsync.back.task.dto;

/**
 * GET /api/projects/{projectId}/tasks/summary 응답. 프로젝트 내 태스크의 상태별 카운트와
 * 완료 비율(progressPercent = done / total * 100, 정수 반올림, total=0이면 0)을 제공한다.
 */
public record TaskSummaryStatsResponse(
		long total,
		long todo,
		long inProgress,
		long review,
		long done,
		int progressPercent
) {
	public static TaskSummaryStatsResponse of(long todo, long inProgress, long review, long done) {
		long total = todo + inProgress + review + done;
		int progressPercent = total == 0 ? 0 : (int) Math.round((done * 100.0) / total);
		return new TaskSummaryStatsResponse(total, todo, inProgress, review, done, progressPercent);
	}
}
