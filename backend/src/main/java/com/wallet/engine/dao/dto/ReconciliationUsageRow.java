package com.wallet.engine.dao.dto;

/**
 * user_benefit_usage 한 행에 혜택의 계산방식을 조인한 조회 투영 — 대사용.
 *
 * 계산방식을 함께 읽는 이유는 횟수의 대조 가능 여부가 거기에 달려 있어서다
 * (COUNT_STEP의 횟수는 원본에 흔적이 없다). 그 해석은 서비스가 한다.
 */
public class ReconciliationUsageRow {

    private long userCardId;
    private long benefitId;
    private String baseYearMonth;
    private long usedAmount;
    private long usedCount;
    /** benefit.calc_method 원값 */
    private String calcMethod;

    public long getUserCardId() {
        return userCardId;
    }

    public void setUserCardId(long userCardId) {
        this.userCardId = userCardId;
    }

    public long getBenefitId() {
        return benefitId;
    }

    public void setBenefitId(long benefitId) {
        this.benefitId = benefitId;
    }

    public String getBaseYearMonth() {
        return baseYearMonth;
    }

    public void setBaseYearMonth(String baseYearMonth) {
        this.baseYearMonth = baseYearMonth;
    }

    public long getUsedAmount() {
        return usedAmount;
    }

    public void setUsedAmount(long usedAmount) {
        this.usedAmount = usedAmount;
    }

    public long getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(long usedCount) {
        this.usedCount = usedCount;
    }

    public String getCalcMethod() {
        return calcMethod;
    }

    public void setCalcMethod(String calcMethod) {
        this.calcMethod = calcMethod;
    }
}
