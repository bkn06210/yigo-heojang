package com.wallet.engine.model;

/**
 * 실적 판정 결과 — 전월실적으로 판정된 구간과 그 통합한도.
 *
 * 카드 단위 계산(CardBenefitSelector)이 이 결과로 CardState를 조립한다:
 *   new CardState(status.performanceMet(), status.sharedMonthlyLimit(), sharedLimitUsed, usages)
 *
 * tierId가 CardState엔 안 들어가지만 여기 있는 이유: benefit_tier_limit 조회 키다.
 * 어느 구간인지를 알아야 그 구간의 개별한도·혜택값(TierLimitResolver 입력)을 가져올 수 있다.
 *
 * performanceMet은 저장 필드가 아니라 파생이다 — (minPerformanceAmount=0, performanceMet=true)
 * 같은 모순 상태를 타입 차원에서 막는다. "require_performance='Y' 충족 = 판정된 구간의
 * min_performance_amount > 0"이 확정 규칙이다.
 *
 * @param tierId               판정된 구간 ID
 * @param minPerformanceAmount 판정된 구간의 최소 전월실적(원)
 * @param sharedMonthlyLimit   판정된 구간의 통합할인한도. null = 통합한도 없음, 0 = 혜택 없음
 */
public record PerformanceStatus(long tierId, long minPerformanceAmount, Long sharedMonthlyLimit) {

    /** 실적 충족 여부. 판정된 구간의 min_performance_amount > 0이면 true */
    public boolean performanceMet() {
        return minPerformanceAmount > 0;
    }
}
