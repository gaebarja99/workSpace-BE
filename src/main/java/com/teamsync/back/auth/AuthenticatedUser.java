package com.teamsync.back.auth;

import com.teamsync.back.user.Role;

/**
 * JWT 클레임에서 복원한, 요청 처리 동안 사용되는 인증 주체.
 * 매 요청마다 DB를 조회하지 않고 토큰의 클레임만으로 인가 판단(역할)과
 * 워크스페이스 스코핑(자기 워크스페이스 데이터만 접근)을 수행하기 위한 경량 principal이다.
 */
public record AuthenticatedUser(
		Long userId,
		Long workspaceId,
		String email,
		Role role
) {
}
