package com.teamsync.back.task;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.task.dto.ChecklistItemCreateRequest;
import com.teamsync.back.task.dto.ChecklistItemResponse;
import com.teamsync.back.task.dto.ChecklistItemUpdateRequest;
import com.teamsync.back.task.dto.MyTaskResponse;
import com.teamsync.back.task.dto.TaskCreateRequest;
import com.teamsync.back.task.dto.TaskResponse;
import com.teamsync.back.task.dto.TaskSummaryResponse;
import com.teamsync.back.task.dto.TaskUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-101(보드) / FR-102(태스크 카드) API.
 * 조회(GET)는 인증된 워크스페이스 구성원이면 GUEST를 포함해 누구나 가능하고,
 * 생성/수정/삭제는 ProjectController와 동일하게 GUEST를 제외한 ADMIN/LEADER/MEMBER만 가능하다.
 */
@RestController
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@PostMapping("/api/projects/{projectId}/tasks")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId, @Valid @RequestBody TaskCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(taskService.createTask(principal, projectId, request));
	}

	@GetMapping("/api/projects/{projectId}/tasks")
	public ResponseEntity<List<TaskSummaryResponse>> list(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long projectId) {
		return ResponseEntity.ok(taskService.listTasks(principal, projectId));
	}

	// FR-104(담당자별 대시보드, US-01 "내 업무"): 정적 경로("me")이므로 Spring MVC가
	// "/api/tasks/{taskId}"보다 이 매핑을 항상 우선 매칭한다(리터럴 경로 세그먼트가 변수 세그먼트보다
	// 더 구체적인 패턴으로 취급됨). 명시성을 위해 {taskId} 매핑보다 앞서 선언한다.
	@GetMapping("/api/tasks/me")
	public ResponseEntity<List<MyTaskResponse>> listMyTasks(@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(taskService.listMyTasks(principal));
	}

	@GetMapping("/api/tasks/{taskId}")
	public ResponseEntity<TaskResponse> get(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long taskId) {
		return ResponseEntity.ok(taskService.getTask(principal, taskId));
	}

	@PatchMapping("/api/tasks/{taskId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<TaskResponse> update(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long taskId, @Valid @RequestBody TaskUpdateRequest request) {
		return ResponseEntity.ok(taskService.updateTask(principal, taskId, request));
	}

	@DeleteMapping("/api/tasks/{taskId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long taskId) {
		taskService.deleteTask(principal, taskId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/api/tasks/{taskId}/checklist-items")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<ChecklistItemResponse> addChecklistItem(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long taskId,
			@Valid @RequestBody ChecklistItemCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(taskService.addChecklistItem(principal, taskId, request));
	}

	@PatchMapping("/api/tasks/{taskId}/checklist-items/{itemId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<ChecklistItemResponse> updateChecklistItem(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long taskId,
			@PathVariable Long itemId, @Valid @RequestBody ChecklistItemUpdateRequest request) {
		return ResponseEntity.ok(taskService.updateChecklistItem(principal, taskId, itemId, request));
	}

	@DeleteMapping("/api/tasks/{taskId}/checklist-items/{itemId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<Void> deleteChecklistItem(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long taskId, @PathVariable Long itemId) {
		taskService.deleteChecklistItem(principal, taskId, itemId);
		return ResponseEntity.noContent().build();
	}
}
