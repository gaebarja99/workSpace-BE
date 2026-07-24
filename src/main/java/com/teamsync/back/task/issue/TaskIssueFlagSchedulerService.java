package com.teamsync.back.task.issue;

import com.teamsync.back.project.Project;
import com.teamsync.back.project.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * FR-406(이슈/리스크 자동 플래그, 완전판) 배치. 매일 새벽 02:00(KST, FR-106 반복 태스크 배치의
 * 새벽 01:00 다음 시간대)에 전체 프로젝트를 순회하며 OVERDUE/STALE/BLOCKED 플래그를 재계산한다.
 *
 * RecurringTaskSchedulerService와 동일한 원칙으로 이 클래스는 트랜잭션을 걸지 않고 순회만 담당하며,
 * 실제 재계산(TaskIssueFlagBatchService#recomputeForProject)은 프로젝트별로 독립된 REQUIRES_NEW
 * 트랜잭션에서 실행되므로 특정 프로젝트에서 예외가 발생해도 그 프로젝트의 변경만 롤백되고 다른
 * 프로젝트에는 영향을 주지 않는다. 실패한 프로젝트는 ERROR 로그로 남긴다.
 */
@Slf4j
@Service
public class TaskIssueFlagSchedulerService {

	private final ProjectRepository projectRepository;
	private final TaskIssueFlagBatchService taskIssueFlagBatchService;

	public TaskIssueFlagSchedulerService(ProjectRepository projectRepository,
			TaskIssueFlagBatchService taskIssueFlagBatchService) {
		this.projectRepository = projectRepository;
		this.taskIssueFlagBatchService = taskIssueFlagBatchService;
	}

	@Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
	public void recomputeIssueFlags() {
		for (Project project : projectRepository.findAll()) {
			Long projectId = project.getId();
			try {
				taskIssueFlagBatchService.recomputeForProject(projectId);
			} catch (Exception e) {
				log.error("FR-406 이슈 플래그 재계산에 실패했습니다. projectId={}", projectId, e);
			}
		}
	}
}
