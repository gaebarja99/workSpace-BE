package com.teamsync.back.task;

import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.task.dto.ChecklistItemCreateRequest;
import com.teamsync.back.task.dto.ChecklistItemResponse;
import com.teamsync.back.task.dto.ChecklistItemUpdateRequest;
import com.teamsync.back.task.dto.MyTaskResponse;
import com.teamsync.back.task.dto.TaskActivityResponse;
import com.teamsync.back.task.dto.TaskCommentRequest;
import com.teamsync.back.task.dto.TaskCommentResponse;
import com.teamsync.back.task.dto.TaskCreateRequest;
import com.teamsync.back.task.dto.TaskDependencyCreateRequest;
import com.teamsync.back.task.dto.TaskDependencyListResponse;
import com.teamsync.back.task.dto.TaskDependencyResponse;
import com.teamsync.back.task.dto.TaskFileLinkRequest;
import com.teamsync.back.task.dto.TaskFileLinkResponse;
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
	private final TaskDependencyService taskDependencyService;

	public TaskController(TaskService taskService, TaskDependencyService taskDependencyService) {
		this.taskService = taskService;
		this.taskDependencyService = taskDependencyService;
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

	// FR-304(US-09): 채널/아카이브 업로드 파일을 재업로드 없이 태스크에 "링크"만 한다.
	@GetMapping("/api/tasks/{taskId}/files")
	public ResponseEntity<List<TaskFileLinkResponse>> listFiles(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long taskId) {
		return ResponseEntity.ok(taskService.listTaskFiles(principal, taskId));
	}

	@PostMapping("/api/tasks/{taskId}/files")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<TaskFileLinkResponse> linkFile(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long taskId, @Valid @RequestBody TaskFileLinkRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(taskService.linkTaskFile(principal, taskId, request));
	}

	@DeleteMapping("/api/tasks/{taskId}/files/{archivedFileId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<Void> unlinkFile(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long taskId, @PathVariable Long archivedFileId) {
		taskService.unlinkTaskFile(principal, taskId, archivedFileId);
		return ResponseEntity.noContent().build();
	}

	// FR-305(US-10): 태스크 댓글 작성 시 연결된 채널 스레드(FR-303)에도 동일 내용이 자동 게시된다.
	@GetMapping("/api/tasks/{taskId}/comments")
	public ResponseEntity<List<TaskCommentResponse>> listComments(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long taskId) {
		return ResponseEntity.ok(taskService.listTaskComments(principal, taskId));
	}

	@PostMapping("/api/tasks/{taskId}/comments")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<TaskCommentResponse> createComment(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long taskId, @Valid @RequestBody TaskCommentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(taskService.createTaskComment(principal, taskId, request));
	}

	// FR-105-B(US-01): 태스크 활동 로그. 조회는 인증된 워크스페이스 구성원이면 누구나 가능하다(createdAt 오름차순).
	@GetMapping("/api/tasks/{taskId}/activities")
	public ResponseEntity<List<TaskActivityResponse>> listActivities(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long taskId) {
		return ResponseEntity.ok(taskService.listTaskActivities(principal, taskId));
	}

	// FR-107(P2, 축소 범위): 태스크 간 선후행 의존관계. 시각화 없이 데이터 모델+CRUD API만 제공한다.
	@GetMapping("/api/tasks/{taskId}/dependencies")
	public ResponseEntity<TaskDependencyListResponse> listDependencies(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long taskId) {
		return ResponseEntity.ok(taskDependencyService.listDependencies(principal, taskId));
	}

	@PostMapping("/api/tasks/{taskId}/dependencies")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<TaskDependencyResponse> createDependency(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long taskId,
			@Valid @RequestBody TaskDependencyCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(taskDependencyService.createDependency(principal, taskId, request));
	}

	@DeleteMapping("/api/tasks/{taskId}/dependencies/{dependencyId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
	public ResponseEntity<Void> deleteDependency(@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable Long taskId, @PathVariable Long dependencyId) {
		taskDependencyService.deleteDependency(principal, taskId, dependencyId);
		return ResponseEntity.noContent().build();
	}
}
