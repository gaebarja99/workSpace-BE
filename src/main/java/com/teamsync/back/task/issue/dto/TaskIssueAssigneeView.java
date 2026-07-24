package com.teamsync.back.task.issue.dto;

import com.teamsync.back.user.User;

/** FR-406: 이슈 목록 응답에 포함되는 담당자 축소 뷰(id/name만). */
public record TaskIssueAssigneeView(Long id, String name) {

	public static TaskIssueAssigneeView from(User user) {
		return new TaskIssueAssigneeView(user.getId(), user.getName());
	}
}
