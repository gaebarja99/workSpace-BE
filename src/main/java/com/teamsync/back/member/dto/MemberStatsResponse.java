package com.teamsync.back.member.dto;

/** 구성원 관리(P1): GET /api/admin/members/stats 응답. */
public record MemberStatsResponse(
		long totalMembers,
		long activeMembers,
		long pendingInvitations,
		long adminCount
) {
}
