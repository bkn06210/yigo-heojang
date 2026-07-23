package com.wallet.engine.calculator;

import com.wallet.engine.model.PerformanceProgress;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.PerformanceTier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 실적 구간 판정기 — 실적 금액으로 어느 구간에 드는지 고른다.
 *
 * 이름은 TierLimitResolver(구간 안의 한도·혜택값 해석)와 짝이다. 이쪽은 구간 자체를 고르고,
 * 저쪽은 고른 구간의 값으로 혜택 규칙을 갈아끼운다.
 *
 * 판정은 "금액 → 구간"의 순수 함수(resolve)다. 이걸 두 축이 공유한다 —
 * 전월실적 축(judge, 실적 충족 판정)과 당월누적 축(progressOf, 진행률). 두 축이 같은
 * 구간 목록 위에 서야 "충족했는데 달성률이 0" 같은 자기모순이 구조적으로 생기지 않는다.
 *
 * Spring·DB·시계에 의존하지 않는 순수 계산기다. "어느 달의 실적인가"는 호출자가 이미 정했다.
 */
public final class PerformanceTierResolver {

    /**
     * 금액이 드는 구간을 고른다 — min_performance_amount <= amount 인 행 중 최댓값.
     *
     * 모든 카드는 0원 구간을 반드시 갖는다는 스키마 전제 덕에 항상 행 하나가 나온다.
     * 0원 구간이 없으면 null로 뭉개지 않고 예외를 던진다 — 시드 오류를 드러내기 위해서다.
     * card_id별 min 중복을 막는 UNIQUE 제약이 있어 최댓값은 늘 유일하다(동점 규칙 불필요).
     */
    public PerformanceTier resolve(List<PerformanceTier> tiers, long amount) {
        if (tiers == null || tiers.isEmpty()) {
            throw new IllegalArgumentException("실적구간 목록은 비어 있을 수 없다");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("실적 금액은 음수일 수 없다: " + amount);
        }
        PerformanceTier best = null;
        for (PerformanceTier tier : tiers) {
            if (tier.minPerformanceAmount() <= amount
                    && (best == null || tier.minPerformanceAmount() > best.minPerformanceAmount())) {
                best = tier;
            }
        }
        if (best == null) {
            throw new IllegalStateException("0원 구간이 없다 — 모든 카드는 min_performance_amount=0 행을 가져야 한다");
        }
        return best;
    }

    /**
     * 전월실적 축 — 현재 적용 구간과 그 통합한도를 판정한다.
     * performanceMet은 PerformanceStatus가 min_performance_amount > 0으로 파생한다.
     */
    public PerformanceStatus judge(List<PerformanceTier> tiers, long prevPerformanceAmount) {
        PerformanceTier tier = resolve(tiers, prevPerformanceAmount);
        return new PerformanceStatus(tier.tierId(), tier.minPerformanceAmount(), tier.sharedMonthlyLimit());
    }

    /**
     * 당월누적 축 — 다음 목표 구간까지 얼마나 왔는지 계산한다(현황 조회 화면용).
     *
     * 목표는 아직 넘지 못한 가장 낮은 구간이고, 전 구간을 이미 넘었으면 최고 구간이다.
     * 목표가 0원이면(구간이 0원 하나뿐 = 실적 조건 없는 카드) 달성률은 0.0이 아니라 null이다 —
     * "실적 조건 없음"과 "실적 0% 달성"은 화면에서 다르게 보여야 한다.
     */
    public PerformanceProgress progressOf(List<PerformanceTier> tiers, long currentPerformanceAmount) {
        if (tiers == null || tiers.isEmpty()) {
            throw new IllegalArgumentException("실적구간 목록은 비어 있을 수 없다");
        }
        if (currentPerformanceAmount < 0) {
            throw new IllegalArgumentException("실적 금액은 음수일 수 없다: " + currentPerformanceAmount);
        }
        long highest = Long.MIN_VALUE;
        Long nextTarget = null;
        for (PerformanceTier tier : tiers) {
            long min = tier.minPerformanceAmount();
            highest = Math.max(highest, min);
            if (min > currentPerformanceAmount && (nextTarget == null || min < nextTarget)) {
                nextTarget = min;
            }
        }
        long target = (nextTarget != null) ? nextTarget : highest;
        long remaining = Math.max(0L, target - currentPerformanceAmount);
        BigDecimal rate = (target == 0)
                ? null
                : BigDecimal.valueOf(currentPerformanceAmount * 100)
                        .divide(BigDecimal.valueOf(target), 1, RoundingMode.HALF_UP);
        return new PerformanceProgress(target, remaining, rate);
    }
}
