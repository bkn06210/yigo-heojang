package com.wallet.engine.dao.dto;

/**
 * user_card_monthly_state 한 행의 조회 투영 — 카드의 월별 실적·소진 현황.
 *
 * 회원의 보유카드를 한 번에 조회하므로 그룹핑 키로 userCardId를, 어느 달 행인지 구분하려고
 * baseYearMonth를 함께 가져온다.
 *
 * <b>전월실적은 저장된 집계값을 읽는다.</b> 지난달 거래를 매번 다시 합산하지 않는다 —
 * prevPerformanceAmount는 지난달 행의 currentPerformanceAmount를 옮겨 담은 값이고
 * (DDL 주석: "당월 누적 실적인정액. 다음 달 전월실적이 된다"), 그 이월은 정산이 이번 달 행을
 * 만들 때 수행한다. 원본 재합산은 스냅샷 생성·보정 경로(PerformanceSnapshotService)의 몫이다.
 */
public class CardMonthlyStateRow {

    private long userCardId;
    private String baseYearMonth;
    private long prevPerformanceAmount;
    private long currentPerformanceAmount;
    private long sharedLimitUsed;

    public long getUserCardId() {
        return userCardId;
    }

    public void setUserCardId(long userCardId) {
        this.userCardId = userCardId;
    }

    public String getBaseYearMonth() {
        return baseYearMonth;
    }

    public void setBaseYearMonth(String baseYearMonth) {
        this.baseYearMonth = baseYearMonth;
    }

    public long getPrevPerformanceAmount() {
        return prevPerformanceAmount;
    }

    public void setPrevPerformanceAmount(long prevPerformanceAmount) {
        this.prevPerformanceAmount = prevPerformanceAmount;
    }

    public long getCurrentPerformanceAmount() {
        return currentPerformanceAmount;
    }

    public void setCurrentPerformanceAmount(long currentPerformanceAmount) {
        this.currentPerformanceAmount = currentPerformanceAmount;
    }

    public long getSharedLimitUsed() {
        return sharedLimitUsed;
    }

    public void setSharedLimitUsed(long sharedLimitUsed) {
        this.sharedLimitUsed = sharedLimitUsed;
    }
}
