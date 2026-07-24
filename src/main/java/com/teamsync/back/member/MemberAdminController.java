package com.teamsync.back.member;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.member.dto.ChangeRoleRequest;
import com.teamsync.back.member.dto.ChangeStatusRequest;
import com.teamsync.back.member.dto.MemberResponse;
import com.teamsync.back.member.dto.MemberStatsResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 구성원 관리(P1): 워크스페이스 구성원 목록/통계 조회, 역할/활성상태 변경(ADMIN 전용). */
@RestController
@RequestMapping("/api/admin/members")
public class MemberAdminController {

	private final MemberService memberService;

	public MemberAdminController(MemberService memberService) {
		this.memberService = memberService;
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<MemberResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(memberService.listMembers(principal));
	}

	@GetMapping("/stats")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<MemberStatsResponse> stats(@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(memberService.getStats(principal));
	}

	@PatchMapping("/{userId}/role")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<MemberResponse> changeRole(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long userId, @Valid @RequestBody ChangeRoleRequest request) {
		return ResponseEntity.ok(memberService.changeRole(principal, userId, request.role()));
	}

	@PatchMapping("/{userId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<MemberResponse> changeStatus(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long userId, @Valid @RequestBody ChangeStatusRequest request) {
		return ResponseEntity.ok(memberService.changeStatus(principal, userId, request.status()));
	}
}
