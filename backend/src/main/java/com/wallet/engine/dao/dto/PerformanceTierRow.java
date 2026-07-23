package com.wallet.engine.dao.dto;

/**
 * performance_tier 한 행의 조회 투영.
 *
 * sharedMonthlyLimit은 Long이다 — NULL(통합한도 없음)과 0(혜택 없음)의 의미가 다르다.
 * 원시 타입으로 받으면 NULL이 0으로 뭉개져 이 구분이 사라진다.
 */
public class PerformanceTierRow {

    private long tierId;
    private long minPerformanceAmount;
    private Long sharedMonthlyLimit;

    public long getTierId() {
        return tierId;
    }

    public void setTierId(long tierId) {
        this.tierId = tierId;
    }

    public long getMinPerformanceAmount() {
        return minPerformanceAmount;
    }

    public void setMinPerformanceAmount(long minPerformanceAmount) {
        this.minPerformanceAmount = minPerformanceAmount;
    }

    public Long getSharedMonthlyLimit() {
        return sharedMonthlyLimit;
    }

    public void setSharedMonthlyLimit(Long sharedMonthlyLimit) {
        this.sharedMonthlyLimit = sharedMonthlyLimit;
    }
}
