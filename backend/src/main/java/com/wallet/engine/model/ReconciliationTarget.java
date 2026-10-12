package com.wallet.engine.model;

/**
 * 대사에서 맞춰 보는 값 — 상태 테이블의 어느 컬럼을 원본의 무엇과 비교하는지.
 *
 * 상태 테이블은 결제마다 더하고 취소마다 빼서 유지하는 누적값이라, 한 번 어긋나면 스스로
 * 돌아오지 않는다. 그래서 값마다 "원본(소비내역)에서 다시 구한 값"을 짝으로 둔다.
 */
public enum ReconciliationTarget {

    /** user_benefit_usage.used_amount ↔ 그 혜택이 적용된 유효 소비내역의 discount_amount 합 */
    BENEFIT_USED_AMOUNT,

    /**
     * user_benefit_usage.used_count ↔ 그 혜택이 적용된 유효 소비내역 건수.
     *
     * COUNT_STEP(스탬프) 혜택은 예외다. 스탬프는 혜택이 지급되지 않은 결제에서도 찍히는데
     * 소비내역에는 적용된 혜택 하나만 남으므로, 횟수를 원본으로 복원할 수 없다.
     */
    BENEFIT_USED_COUNT,

    /** user_card_monthly_state.shared_limit_used ↔ 통합한도를 쓰는 혜택이 적용된 유효 소비내역의 discount_amount 합 */
    SHARED_LIMIT_USED,

    /** user_card_monthly_state.current_performance_amount ↔ 그 달 소비내역을 실적 규칙으로 다시 합산한 값 */
    CURRENT_PERFORMANCE,

    /**
     * user_card_monthly_state.prev_performance_amount ↔ 직전 월 행의 current_performance_amount.
     *
     * 이 값만 짝이 소비내역이 아니라 상태 테이블의 다른 행이다. 이월이 제대로 됐는지를 보며,
     * 직전 월 누적 자체가 맞는지는 그 달의 {@link #CURRENT_PERFORMANCE} 대조가 본다.
     */
    PREV_PERFORMANCE
}
