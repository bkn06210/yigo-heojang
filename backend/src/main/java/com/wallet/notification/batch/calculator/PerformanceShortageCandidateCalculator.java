package com.wallet.notification.batch.calculator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import com.wallet.notification.batch.model.PerformanceShortageCandidate;
import com.wallet.notification.batch.model.PerformanceShortageCandidateRow;
import com.wallet.notification.batch.model.PerformanceShortageTrigger;
import com.wallet.notification.mapper.PerformanceShortageCandidateMapper;

/**
 * 실적 부족 알림 후보를 계산한다 (규칙 문서 2장).
 */
@Component
@RequiredArgsConstructor
public class PerformanceShortageCandidateCalculator {
    private static final int RATE_80_PERCENT = 80;
    private static final int RATE_100_PERCENT = 100;

    // 정렬 기준(2.5절): achievementRate 내림차순 → targetPerformance 내림차순 → userCardId 오름차순.
    private static final Comparator<PerformanceShortageCandidate> CANDIDATE_ORDER =
        Comparator.comparingDouble(PerformanceShortageCandidate::achievementRate).reversed()
            .thenComparing(Comparator.comparingLong(PerformanceShortageCandidate::targetPerformance).reversed())
            .thenComparingLong(PerformanceShortageCandidate::userCardId);

    private final PerformanceShortageCandidateMapper mapper;
    private final Clock clock;

    public List<PerformanceShortageCandidate> calculate() {
        LocalDate today = LocalDate.now(clock);
        YearMonth currentYearMonth = YearMonth.from(today);

        // YearMonth.atEndOfMonth()는 2월이 28일인지 29일인지(윤년 여부), 30일/31일인지를
        // 달력 규칙에 맞춰 스스로 계산해준다. "말일 - 7일"을 직접 계산하지 않는다.
        LocalDate monthEndDate = currentYearMonth.atEndOfMonth();
        long daysUntilMonthEnd = ChronoUnit.DAYS.between(today, monthEndDate);

        Optional<PerformanceShortageTrigger> trigger = resolveTrigger(daysUntilMonthEnd);
        if (trigger.isEmpty()) {
            // 오늘이 D-7도 D-3도 아니면 조회할 필요조차 없다.
            return List.of();
        }

        String baseYearMonth = currentYearMonth.toString(); // YearMonth의 기본 toString은 "2026-08" 형태
        List<PerformanceShortageCandidateRow> rows = mapper.findCandidateRows(baseYearMonth);

        return rows.stream()
            .filter(row -> matchesTrigger(row, trigger.get()))
            .map(row -> toCandidate(row, trigger.get(), baseYearMonth))
            .sorted(CANDIDATE_ORDER)
            .toList();
    }

    private Optional<PerformanceShortageTrigger> resolveTrigger(long daysUntilMonthEnd) {
        for (PerformanceShortageTrigger candidate : PerformanceShortageTrigger.values()) {
            if (daysUntilMonthEnd == candidate.getDaysBeforeMonthEnd()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private boolean matchesTrigger(PerformanceShortageCandidateRow row, PerformanceShortageTrigger trigger) {
        boolean atLeast80 = isAtLeastPercent(row, RATE_80_PERCENT);
        boolean atLeast100 = isAtLeastPercent(row, RATE_100_PERCENT);

        if (trigger == PerformanceShortageTrigger.D7) {
            return !atLeast80;
        }
        // D3: 80% 이상 100% 미만
        return atLeast80 && !atLeast100;
    }

    /**
     * "달성률이 percent% 이상인가"를 반올림 없이 정수 곱셈으로 판정한다.
     * (현재실적 / 목표실적) * 100 처럼 나눗셈을 먼저 하면 소수점이 잘려서
     * 경계값(e.g. 정확히 80.0%)이 잘못 판정될 수 있다. 현재실적 * 100 >= 목표실적 * percent
     * 형태의 교차 곱셈은 나눗셈이 없어 이 문제가 없다(규칙 문서 1.3절과 같은 원칙).
     */
    private boolean isAtLeastPercent(PerformanceShortageCandidateRow row, int percent) {
        return row.getCurrentPerformanceAmount() * 100L
            >= row.getTargetPerformance() * (long) percent;
    }

    private PerformanceShortageCandidate toCandidate(
        PerformanceShortageCandidateRow row,
        PerformanceShortageTrigger trigger,
        String yearMonth
    ) {
        long target = row.getTargetPerformance();
        long current = row.getCurrentPerformanceAmount();
        long remaining = Math.max(target - current, 0);
        double achievementRate = current * 100.0 / target;

        String deduplicationKey = "PERF_SHORTAGE:%d:MONTH:%s:%s"
            .formatted(row.getUserCardId(), yearMonth, trigger.name());

        return new PerformanceShortageCandidate(
            row.getMemberId(),
            row.getUserCardId(),
            row.getCardId(),
            row.getCardName(),
            target,
            current,
            remaining,
            achievementRate,
            trigger,
            yearMonth,
            deduplicationKey
        );
    }
}