package com.wallet.engine.dao.dto;

/**
 * 혜택이 적용된 소비내역을 (보유카드, 혜택, 월)로 묶은 합계 — user_benefit_usage 한 행의 원본 쪽 짝.
 *
 * 취소 건만 있는 묶음도 행으로 나온다(합계·건수는 0). "적용된 기록이 아예 없다"와
 * "있었는데 전부 취소됐다"를 가르기 위해서다.
 */
public class ReconciliationBenefitSumRow {

    private long userCardId;
    private long benefitId;
    /** 결제일시에서 뽑은 연월 (YYYY-MM). 상태 테이블의 base_year_month와 같은 형식 */
    private String baseYearMonth;
    /** 취소되지 않은 건의 discount_amount 합 */
    private long discountAmount;
    /** 취소되지 않은 건수 */
    private long appliedCount;
    /** benefit.use_shared_limit 원값 'Y'/'N' */
    private String useSharedLimit;
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

    public long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public long getAppliedCount() {
        return appliedCount;
    }

    public void setAppliedCount(long appliedCount) {
        this.appliedCount = appliedCount;
    }

    public String getUseSharedLimit() {
        return useSharedLimit;
    }

    public void setUseSharedLimit(String useSharedLimit) {
        this.useSharedLimit = useSharedLimit;
    }

    public String getCalcMethod() {
        return calcMethod;
    }

    public void setCalcMethod(String calcMethod) {
        this.calcMethod = calcMethod;
    }
}
