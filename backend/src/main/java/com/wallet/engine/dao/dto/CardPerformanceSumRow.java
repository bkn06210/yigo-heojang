package com.wallet.engine.dao.dto;

/**
 * 보유카드 하나의 기간 합산 실적인정액 — user_card_monthly_state의 여러 달 행을 SUM한 결과.
 *
 * 전분기 실적 판정에 쓴다. 전월실적과 같은 방식으로 저장된 집계값을 읽을 뿐
 * 거래를 재합산하지 않는다 — 카드 수만큼 거래 전량을 스캔하는 비용을 피하기 위한 기존 방침이다.
 */
public class CardPerformanceSumRow {

    private long userCardId;
    private long performanceAmount;

    public long getUserCardId() {
        return userCardId;
    }

    public void setUserCardId(long userCardId) {
        this.userCardId = userCardId;
    }

    public long getPerformanceAmount() {
        return performanceAmount;
    }

    public void setPerformanceAmount(long performanceAmount) {
        this.performanceAmount = performanceAmount;
    }
}
