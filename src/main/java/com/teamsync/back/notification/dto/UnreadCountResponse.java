package com.teamsync.back.notification.dto;

/**
 * GET /api/notifications/me/unread-count 응답.
 */
public record UnreadCountResponse(long count) {
}
