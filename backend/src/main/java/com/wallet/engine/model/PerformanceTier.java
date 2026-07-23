package com.wallet.engine.model;

/**
 * 실적구간 한 행 (performance_tier 한 행).
 *
 * 판정 규칙: min_performance_amount <= 전월실적 인 행 중 min_performance_amount 최댓값.
 * 모든 카드는 min_performance_amount = 0 행을 반드시 1개 가지므로 판정은 항상 행 하나를 반환한다.
 *
 * sharedMonthlyLimit은 Long이다 — NULL(통합한도 없음, 개별한도만 적용)과 0(혜택 없음)의
 * 의미가 다르다. 원시 타입으로 받으면 이 구분이 사라진다.
 *
 * @param tierId               실적구간 ID
 * @param minPerformanceAmount 이 구간의 최소 전월실적(원). 조건 없으면 0
 * @param sharedMonthlyLimit   통합할인한도(월, 원). null = 통합한도 없음, 0 = 혜택 없음
 */
public record PerformanceTier(long tierId, long minPerformanceAmount, Long sharedMonthlyLimit) {

    public PerformanceTier {
        if (minPerformanceAmount < 0) {
            throw new IllegalArgumentException("최소 전월실적은 음수일 수 없다: " + minPerformanceAmount);
        }
    }
}
