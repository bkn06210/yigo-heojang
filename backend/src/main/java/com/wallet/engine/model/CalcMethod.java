package com.wallet.engine.model;

/**
 * 혜택 계산 방식 (benefit.calc_method).
 * RATE = 정률(%): 대상금액 × 율. FIXED = 정액(원): 고정값.
 * COUNT_STEP = N회마다 정액: 조건을 충족한 결제가 step_count번 쌓일 때마다 정액 지급(스탬프형).
 *
 * isEstimate 판정의 출발점 — 금액이 결제액에 비례하는 RATE만 예상일 수 있고,
 * FIXED·COUNT_STEP은 결제금액이 달라져도 값이 안 변하므로 항상 확정이다.
 *
 * COUNT_STEP을 FIXED로 넣으면 결제마다 지급되어 실제의 step_count배가 나간다.
 */
public enum CalcMethod {
    RATE,
    FIXED,
    COUNT_STEP
}
