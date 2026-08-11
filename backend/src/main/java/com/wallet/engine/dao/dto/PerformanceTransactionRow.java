package com.wallet.engine.dao.dto;

/**
 * expense 한 행의 조회 투영 — DB 원값 그대로의 flat row.
 *
 * 이 클래스는 DB 행의 충실한 거울이다. 'Y'/'N'·payment_status 문자열을 boolean으로 접거나
 * 대상 유형을 enum으로 바꾸는 해석은 여기서 하지 않는다 — 그 변환은 PerformanceInputAssembler가
 * 담당한다(변환 규칙을 자바 한 곳에 모으기 위함). MyBatis가 setter로 채우므로 no-arg + 가변이다.
 */
public class PerformanceTransactionRow {

    private long expenseId;
    private long amount;
    private String paymentStatus;
    private String categoryCode;
    /** 상위(대분류) 카테고리 코드. 대분류면 NULL(LEFT JOIN 미매칭) */
    private String parentCategoryCode;
    private String paymentType;
    /** 무이자할부 여부 원값 'Y'/'N'. boolean 변환은 assembler가 한다 */
    private String isInterestFree;
    private long discountAmount;
    /**
     * 적용 혜택의 실적 제외 지정 원값 'Y'/'N' (benefit 조인).
     * 혜택 미적용 거래는 NULL이고, assembler가 false로 접는다.
     */
    private String excludeFromPerformance;

    public long getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(long expenseId) {
        this.expenseId = expenseId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getParentCategoryCode() {
        return parentCategoryCode;
    }

    public void setParentCategoryCode(String parentCategoryCode) {
        this.parentCategoryCode = parentCategoryCode;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getIsInterestFree() {
        return isInterestFree;
    }

    public void setIsInterestFree(String isInterestFree) {
        this.isInterestFree = isInterestFree;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getExcludeFromPerformance() {
        return excludeFromPerformance;
    }

    public void setExcludeFromPerformance(String excludeFromPerformance) {
        this.excludeFromPerformance = excludeFromPerformance;
    }
}
