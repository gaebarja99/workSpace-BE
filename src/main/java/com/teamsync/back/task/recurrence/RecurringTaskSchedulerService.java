package com.teamsync.back.task.recurrence;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * FR-106(반복 태스크 템플릿 자동 생성) 배치. 매일 새벽 01:00(KST)에 active=true인 모든 템플릿을
 * 순회하며, 오늘이 그 템플릿의 생성일에 해당하고 이번 주기(주/월)에 아직 생성하지 않았다면 Task를
 * 1건 생성한다.
 *
 * 이 클래스는 트랜잭션을 걸지 않고 순회/판단(shouldGenerateToday)만 담당한다. 실제 Task 생성
 * (RecurringTaskGenerationService#generateTask)은 템플릿별로 독립된 REQUIRES_NEW 트랜잭션에서
 * 실행되므로, 특정 템플릿에서 예외(알림 발송 실패, DB 제약 위반 등)가 발생해도 그 템플릿의 변경만
 * 롤백되고 같은 배치 실행에서 이미 처리된/이후에 처리될 다른 템플릿에는 영향을 주지 않는다(QA
 * Major 결함 수정: 배치 전체를 감싸는 단일 트랜잭션이었던 이전 구현에서는 한 템플릿의 실패가 전체
 * 롤백을 유발했다). 실패한 템플릿은 ERROR 로그로 남겨 운영 중 추적 가능하게 한다.
 *
 * FR-302(채널 시스템 메시지 자동 게시)는 이 배치의 범위 밖이다 — 계약 문서가 명시적으로 요구한
 * notifyTaskAssigned/recordCreated만 재사용하고, TaskService 전용 채널 게시 로직까지 확장하지
 * 않는다(임의 선행 작업 확장 금지 원칙).
 */
@Slf4j
@Service
public class RecurringTaskSchedulerService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final RecurringTaskTemplateRepository recurringTaskTemplateRepository;
	private final RecurringTaskGenerationService recurringTaskGenerationService;

	public RecurringTaskSchedulerService(RecurringTaskTemplateRepository recurringTaskTemplateRepository,
			RecurringTaskGenerationService recurringTaskGenerationService) {
		this.recurringTaskTemplateRepository = recurringTaskTemplateRepository;
		this.recurringTaskGenerationService = recurringTaskGenerationService;
	}

	@Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
	public void generateRecurringTasks() {
		LocalDate today = LocalDate.now(KST);
		for (RecurringTaskTemplate template : recurringTaskTemplateRepository.findAllByActiveTrue()) {
			if (!shouldGenerateToday(template, today)) {
				continue;
			}
			try {
				recurringTaskGenerationService.generateTask(template.getId(), today);
				log.info("FR-106 반복 태스크를 자동 생성했습니다. templateId={}", template.getId());
			} catch (Exception e) {
				log.error("FR-106 반복 태스크 생성에 실패했습니다. templateId={}", template.getId(), e);
			}
		}
	}

	// package-private: 단위 테스트(RecurringTaskSchedulerServiceTest)가 임의의 today 값으로
	// 월말 클램핑/7일 윈도우 로직을 직접 검증할 수 있도록 접근 제한을 완화했다(동작 변경 없음).
	boolean shouldGenerateToday(RecurringTaskTemplate template, LocalDate today) {
		if (template.getRecurrenceType() == RecurrenceType.WEEKLY) {
			if (today.getDayOfWeek() != template.getDayOfWeek()) {
				return false;
			}
			return !generatedWithinLast7Days(template.getLastGeneratedAt(), today);
		}

		// MONTHLY: 해당 월에 dayOfMonth가 없으면(예: 31일인데 2월) 그 달의 마지막 날로 클램핑한다.
		int lastDayOfMonth = YearMonth.from(today).lengthOfMonth();
		int targetDay = Math.min(template.getDayOfMonth(), lastDayOfMonth);
		if (today.getDayOfMonth() != targetDay) {
			return false;
		}
		return !generatedThisMonth(template.getLastGeneratedAt(), today);
	}

	/** lastGeneratedAt이 없거나(null) 오늘 기준 지난 7일(0~6일 전, 오늘 포함) 이내가 아니면 이번 주에 아직 생성되지 않은 것이다. */
	private boolean generatedWithinLast7Days(LocalDate lastGeneratedAt, LocalDate today) {
		if (lastGeneratedAt == null) {
			return false;
		}
		long daysSince = ChronoUnit.DAYS.between(lastGeneratedAt, today);
		return daysSince >= 0 && daysSince < 7;
	}

	private boolean generatedThisMonth(LocalDate lastGeneratedAt, LocalDate today) {
		return lastGeneratedAt != null
				&& lastGeneratedAt.getYear() == today.getYear()
				&& lastGeneratedAt.getMonth() == today.getMonth();
	}
}
