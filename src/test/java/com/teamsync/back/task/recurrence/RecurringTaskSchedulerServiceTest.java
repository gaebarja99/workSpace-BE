package com.teamsync.back.task.recurrence;

import static org.assertj.core.api.Assertions.assertThat;

import com.teamsync.back.task.TaskPriority;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * FR-106 배치의 생성 여부 판단(shouldGenerateToday) 단위 테스트. QA Minor 지적사항: 월말
 * 클램핑(dayOfMonth=31인데 2월인 경우)과 WEEKLY 7일 윈도우(중복 생성 방지)에 대한 테스트가
 * 없었다. 실제 스케줄(cron)이나 트랜잭션은 검증 대상이 아니므로 임의의 today 값을 직접 주입할 수
 * 있도록 shouldGenerateToday를 package-private으로 열어 직접 호출한다.
 */
@ExtendWith(MockitoExtension.class)
class RecurringTaskSchedulerServiceTest {

	@Mock
	private RecurringTaskTemplateRepository recurringTaskTemplateRepository;

	@Mock
	private RecurringTaskGenerationService recurringTaskGenerationService;

	private final RecurringTaskSchedulerService schedulerService =
			new RecurringTaskSchedulerService(recurringTaskTemplateRepository, recurringTaskGenerationService);

	@Test
	void MONTHLY_dayOfMonth가_31인_템플릿은_2월에는_28일에_생성된다() {
		RecurringTaskTemplate template = monthlyTemplate(31, null);
		LocalDate feb28_2026 = LocalDate.of(2026, 2, 28); // 2026년은 평년(28일까지)

		assertThat(schedulerService.shouldGenerateToday(template, feb28_2026)).isTrue();
		// 2월 27일은 아직 클램핑된 대상일(28일)이 아니므로 생성되지 않는다.
		assertThat(schedulerService.shouldGenerateToday(template, LocalDate.of(2026, 2, 27))).isFalse();
	}

	@Test
	void MONTHLY_같은_달에_이미_생성했으면_다시_생성하지_않는다() {
		RecurringTaskTemplate template = monthlyTemplate(31, LocalDate.of(2026, 2, 28));
		LocalDate feb28_2026 = LocalDate.of(2026, 2, 28);

		assertThat(schedulerService.shouldGenerateToday(template, feb28_2026)).isFalse();
	}

	@Test
	void WEEKLY_정상_7일_주기에서는_생성일에_정확히_한_번만_생성된다() {
		// 매주 월요일 생성 템플릿, 지난 생성일이 정확히 7일 전(직전 월요일)이라면 이번 주 생성 대상이다.
		RecurringTaskTemplate template = weeklyTemplate(DayOfWeek.MONDAY, LocalDate.of(2026, 7, 13));
		LocalDate thisMonday = LocalDate.of(2026, 7, 20);

		assertThat(schedulerService.shouldGenerateToday(template, thisMonday)).isTrue();
	}

	@Test
	void WEEKLY_오늘_이미_생성했다면_같은_날_중복_생성하지_않는다() {
		RecurringTaskTemplate template = weeklyTemplate(DayOfWeek.MONDAY, LocalDate.of(2026, 7, 20));
		LocalDate thisMonday = LocalDate.of(2026, 7, 20);

		assertThat(schedulerService.shouldGenerateToday(template, thisMonday)).isFalse();
	}

	@Test
	void WEEKLY_생성_요일이_아니면_생성하지_않는다() {
		RecurringTaskTemplate template = weeklyTemplate(DayOfWeek.MONDAY, null);
		LocalDate tuesday = LocalDate.of(2026, 7, 21);

		assertThat(schedulerService.shouldGenerateToday(template, tuesday)).isFalse();
	}

	private RecurringTaskTemplate monthlyTemplate(int dayOfMonth, LocalDate lastGeneratedAt) {
		RecurringTaskTemplate template = new RecurringTaskTemplate(null, "월간 보고서 작성", null, TaskPriority.MEDIUM,
				Collections.emptySet(), RecurrenceType.MONTHLY, null, dayOfMonth, 3, true, null);
		if (lastGeneratedAt != null) {
			template.markGenerated(lastGeneratedAt);
		}
		return template;
	}

	private RecurringTaskTemplate weeklyTemplate(DayOfWeek dayOfWeek, LocalDate lastGeneratedAt) {
		RecurringTaskTemplate template = new RecurringTaskTemplate(null, "주간 회의록 정리", null, TaskPriority.MEDIUM,
				Collections.emptySet(), RecurrenceType.WEEKLY, dayOfWeek, null, 1, true, null);
		if (lastGeneratedAt != null) {
			template.markGenerated(lastGeneratedAt);
		}
		return template;
	}
}
