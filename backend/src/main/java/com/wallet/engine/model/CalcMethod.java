package com.wallet.engine.model;

/**
 * 혜택 계산 방식 (benefit.calc_method).
 * RATE = 정률(%): 대상금액 × 율. FIXED = 정액(원): 고정값.
 * isEstimate 판정의 출발점 — FIXED는 항상 확정, RATE는 금액이 구간 입력이면 예상.
 */
public enum CalcMethod {
    RATE,
    FIXED
}
