package com.wallet.engine.dao.dto;

/**
 * 현황(CardMonthlyStatus) 조립에 필요한 카드 월 상태 + 카드명 투영.
 *
 * user_card_monthly_state의 실적·소진 숫자에 card.card_name을 조인해 함께 읽는다 —
 * 현황 응답이 카드명을 요구하는데 상태 테이블엔 없기 때문이다.
 * 목표금액·달성률·남은실적 같은 파생값은 담지 않는다(조회 시 계산). MyBatis가 setter로 채운다.
 */
public class MonthlyStatusRow {

    private long userCardId;
    private String cardName;
    private long prevPerformanceAmount;
    private long currentPerformanceAmount;
    private long sharedLimitUsed;

    public long getUserCardId() {
        return userCardId;
    }

    public void setUserCardId(long userCardId) {
        this.userCardId = userCardId;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
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
