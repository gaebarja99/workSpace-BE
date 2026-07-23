package com.teamsync.back.notification;

import com.teamsync.back.task.TaskStatus;
import java.util.Map;

/**
 * FR-108 상태 변경 알림 메시지에 쓰이는 TaskStatus 한글 라벨.
 * TaskStatus에 새 값이 추가되면 이 맵도 함께 갱신해야 한다(누락 시 enum 이름을 그대로 노출).
 */
final class TaskStatusLabels {

	private static final Map<TaskStatus, String> LABELS = Map.of(
			TaskStatus.TODO, "할 일",
			TaskStatus.IN_PROGRESS, "진행 중",
			TaskStatus.REVIEW, "검토",
			TaskStatus.DONE, "완료");

	private TaskStatusLabels() {
	}

	static String of(TaskStatus status) {
		return LABELS.getOrDefault(status, status.name());
	}
}
