package com.wallet.notification.batch.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.notification.batch.model.PerformanceShortageCandidate;
import com.wallet.notification.batch.model.PerformanceShortageCandidateRow;
import com.wallet.notification.batch.model.PerformanceShortageTrigger;
import com.wallet.notification.mapper.PerformanceShortageCandidateMapper;

class PerformanceShortageCandidateCalculatorTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private PerformanceShortageCandidateMapper mapper;

    // 테스트마다 "오늘"이 달라져야 하므로 @BeforeEach 대신, 원하는 날짜로 매번 새로 만든다.
    private PerformanceShortageCandidateCalculator calculatorAt(LocalDate date) {
        // 이 계산기는 시각이 아니라 날짜만 쓰므로, 하루 중 몇 시로 고정하든 결과는 같다.
        Clock clock = Clock.fixed(date.atStartOfDay(KST).toInstant(), KST);
        return new PerformanceShortageCandidateCalculator(mapper, clock);
    }

    private PerformanceShortageCandidateRow row(
        long userCardId,
        long memberId,
        long cardId,
        String cardName,
        long targetPerformance,
        long currentPerformanceAmount
    ) {
        return PerformanceShortageCandidateRow.builder()
            .userCardId(userCardId)
            .memberId(memberId)
            .cardId(cardId)
            .cardName(cardName)
            .targetPerformance(targetPerformance)
            .currentPerformanceAmount(currentPerformanceAmount)
            .build();
    }

    @Test
    @DisplayName("31일까지 있는 달(8월) - 월말 7일 전이면 D7 후보를 만든다")
    void calculate_success_d7_in31DayMonth() {
        // given: 2026-08-31이 말일이므로 7일 전은 2026-08-24다.
        mapper = mock(PerformanceShortageCandidateMapper.class);
        when(mapper.findCandidateRows("2026-08"))
            .thenReturn(List.of(row(10L, 1L, 100L, "카드A", 300_000L, 100_000L))); // 33.3%

        // when
        List<PerformanceShortageCandidate> result =
            calculatorAt(LocalDate.of(2026, 8, 24)).calculate();

        // then
        assertThat(result).hasSize(1);
        PerformanceShortageCandidate candidate = result.get(0);
        assertThat(candidate.trigger()).isEqualTo(PerformanceShortageTrigger.D7);
        assertThat(candidate.deduplicationKey()).isEqualTo("PERF_SHORTAGE:10:MONTH:2026-08:D7");
        assertThat(candidate.remainingPerformance()).isEqualTo(200_000L);
    }

    @Test
    @DisplayName("31일까지 있는 달(8월) - 월말 3일 전이면 80~100% 미만 후보만 D3로 만든다")
    void calculate_success_d3_in31DayMonth() {
        // given: 2026-08-31이 말일이므로 3일 전은 2026-08-28다.
        mapper = mock(PerformanceShortageCandidateMapper.class);
        when(mapper.findCandidateRows("2026-08"))
            .thenReturn(List.of(row(11L, 1L, 100L, "카드B", 300_000L, 270_000L))); // 90%

        // when
        List<PerformanceShortageCandidate> result =
            calculatorAt(LocalDate.of(2026, 8, 28)).calculate();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).trigger()).isEqualTo(PerformanceShortageTrigger.D3);
        assertThat(result.get(0).deduplicationKey()).isEqualTo("PERF_SHORTAGE:11:MONTH:2026-08:D3");
    }

    @Test
    @DisplayName("30일까지 있는 달(4월)에서도 말일 기준으로 D7/D3를 정확히 계산한다")
    void calculate_success_in30DayMonth() {
        // given: 2026-04-30이 말일이므로 7일 전은 04-23, 3일 전은 04-27이다.
        mapper = mock(PerformanceShortageCandidateMapper.class);
        when(mapper.findCandidateRows("2026-04"))
            .thenReturn(List.of(row(20L, 1L, 200L, "카드C", 400_000L, 100_000L)));

        // when
        List<PerformanceShortageCandidate> d7Result =
            calculatorAt(LocalDate.of(2026, 4, 23)).calculate();
        List<PerformanceShortageCandidate> nonTriggerResult =
            calculatorAt(LocalDate.of(2026, 4, 24)).calculate();

        // then
        assertThat(d7Result).extracting(PerformanceShortageCandidate::trigger)
            .containsExactly(PerformanceShortageTrigger.D7);
        assertThat(nonTriggerResult).isEmpty();
    }

    @Test
    @DisplayName("2월(평년, 28일)에서도 말일 기준으로 D7/D3 날짜를 정확히 계산한다")
    void calculate_success_inFebruaryNonLeapYear() {
        // given: 2025년은 평년이라 2월이 28일까지다. 말일 02-28 기준 D7=02-21, D3=02-25.
        mapper = mock(PerformanceShortageCandidateMapper.class);
        when(mapper.findCandidateRows("2025-02"))
            .thenReturn(List.of(row(30L, 1L, 300L, "카드D", 200_000L, 50_000L)));

        // when
        List<PerformanceShortageCandidate> d7Result =
            calculatorAt(LocalDate.of(2025, 2, 21)).calculate();
        List<PerformanceShortageCandidate> nonTriggerResult =
            calculatorAt(LocalDate.of(2025, 2, 22)).calculate();

        // then
        assertThat(d7Result).hasSize(1);
        assertThat(d7Result.get(0).trigger()).isEqualTo(PerformanceShortageTrigger.D7);
        assertThat(nonTriggerResult).isEmpty();
    }

    @Test
    @DisplayName("2월(윤년, 29일)에서는 하루 밀려서 D7/D3 날짜가 계산된다")
    void calculate_success_inFebruaryLeapYear() {
        // given: 2028년은 윤년이라 2월이 29일까지다. 말일 02-29 기준
        // D7=02-22, D3=02-26 (평년보다 하루씩 뒤로 밀린다).
        mapper = mock(PerformanceShortageCandidateMapper.class);
        when(mapper.findCandidateRows("2028-02"))
            .thenReturn(List.of(row(40L, 1L, 400L, "카드E", 200_000L, 180_000L))); // 90%

        // when
        List<PerformanceShortageCandidate> d3Result =
            calculatorAt(LocalDate.of(2028, 2, 26)).calculate();
        // 평년이라면 D3였을 02-25는, 윤년에서는 아직 D-4라 후보가 없어야 한다.
        List<PerformanceShortageCandidate> nonTriggerResult =
            calculatorAt(LocalDate.of(2028, 2, 25)).calculate();

        // then
        assertThat(d3Result).hasSize(1);
        assertThat(d3Result.get(0).trigger()).isEqualTo(PerformanceShortageTrigger.D3);
        assertThat(nonTriggerResult).isEmpty();
    }

    @Test
    @DisplayName("D-7도 D-3도 아닌 날에는 조회조차 하지 않는다")
    void calculate_success_skipsQueryOnNonTriggerDay() {
        // given
        mapper = mock(PerformanceShortageCandidateMapper.class);

        // when: 8월 31일 기준 D-16인 날
        List<PerformanceShortageCandidate> result =
            calculatorAt(LocalDate.of(2026, 8, 15)).calculate();

        // then
        assertThat(result).isEmpty();
        verify(mapper, never()).findCandidateRows(anyString());
    }

    @Test
    @DisplayName("달성률이 정확히 80%면 D7에서는 제외되고 D3 대상이 된다 (경계값)")
    void calculate_boundary_exactly80Percent() {
        // given: target=300,000 / current=240,000 → 정확히 80%
        mapper = mock(PerformanceShortageCandidateMapper.class);
        when(mapper.findCandidateRows("2026-08"))
            .thenReturn(List.of(row(50L, 1L, 500L, "카드F", 300_000L, 240_000L)));

        // when
        List<PerformanceShortageCandidate> d7Result =
            calculatorAt(LocalDate.of(2026, 8, 24)).calculate();
        List<PerformanceShortageCandidate> d3Result =
            calculatorAt(LocalDate.of(2026, 8, 28)).calculate();

        // then: D7은 80% 미만만 대상이라 정확히 80%는 제외, D3는 80% 이상이 대상이라 포함
        assertThat(d7Result).isEmpty();
        assertThat(d3Result).hasSize(1);
    }

    @Test
    @DisplayName("달성률이 정확히 100%면 D3 대상에서도 제외된다 (경계값)")
    void calculate_boundary_exactly100Percent() {
        // given: target == current
        mapper = mock(PerformanceShortageCandidateMapper.class);
        when(mapper.findCandidateRows("2026-08"))
            .thenReturn(List.of(row(60L, 1L, 600L, "카드G", 300_000L, 300_000L)));

        // when
        List<PerformanceShortageCandidate> d3Result =
            calculatorAt(LocalDate.of(2026, 8, 28)).calculate();

        // then
        assertThat(d3Result).isEmpty();
    }

    @Test
    @DisplayName("정렬 - 달성률 내림차순, 동률이면 목표실적 내림차순, 그것도 같으면 userCardId 오름차순")
    void calculate_success_sortOrder() {
        // given: 모두 8월 D7 날짜(08-24), 모두 80% 미만
        mapper = mock(PerformanceShortageCandidateMapper.class);

        PerformanceShortageCandidateRow lowRate =
            row(5L, 1L, 500L, "카드C", 500_000L, 100_000L);    // 20%
        PerformanceShortageCandidateRow highRate =
            row(20L, 1L, 500L, "카드B", 1_000_000L, 500_000L); // 50%
        PerformanceShortageCandidateRow tieHigherTarget =
            row(15L, 1L, 500L, "카드A", 500_000L, 200_000L);   // 40%, target 500,000
        PerformanceShortageCandidateRow tieLowerTarget =
            row(30L, 1L, 500L, "카드D", 400_000L, 160_000L);   // 40%, target 400,000

        when(mapper.findCandidateRows("2026-08")).thenReturn(
            List.of(lowRate, highRate, tieHigherTarget, tieLowerTarget)
        );

        // when
        List<PerformanceShortageCandidate> result =
            calculatorAt(LocalDate.of(2026, 8, 24)).calculate();

        // then: 50% > 40%(target 500,000) > 40%(target 400,000) > 20%
        assertThat(result)
            .extracting(PerformanceShortageCandidate::userCardId)
            .containsExactly(20L, 15L, 30L, 5L);
    }
}