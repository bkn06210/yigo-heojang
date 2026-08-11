package com.wallet.engine.dao.dto;

import java.time.LocalDateTime;

/**
 * 취소 정산에 필요한 소비내역 한 행의 조회 투영 — DB 원값 그대로의 flat row.
 *
 * 취소는 <b>저장된 값을 역산</b>하므로(재계산 아님) 이 거래가 가산 때 기여한 값을 그대로 읽는다:
 *   · amount / paymentType / isInterestFree / categoryCode — 실적 인정분이었는지 단건 판정용
 *   · appliedBenefitId / discountAmount — 혜택별·통합한도 소진 역산용
 *   · useSharedLimit — 적용 혜택이 통합한도를 썼는지(benefit 조인). 통합한도 차감 여부를 가른다
 *   · paymentDate — 당월 여부 판정과 상태 행의 base_year_month 키
 *
 * 'Y'/'N'·payment_status 문자열을 boolean으로 접는 해석은 여기서 하지 않는다 — 서비스/assembler의 몫이다.
 * MyBatis가 setter로 채우므로 no-arg + 가변이다.
 */
public class ExpenseRow {

    private long expenseId;
    private long userCardId;
    /** 카드 마스터 ID. 실적 제외 규칙·혜택 조회 키 (user_card 조인) */
    private long cardId;
    /** 가맹점 ID. 미등록 가맹점이면 NULL. 스탬프 되돌림의 대상 매칭에 쓴다 */
    private Long merchantId;
    private String merchantCode;
    private long categoryId;
    private String categoryCode;
    /** 상위(대분류) 카테고리 ID. 대분류면 NULL */
    private Long parentCategoryId;
    /** 상위(대분류) 카테고리 코드. 대분류면 NULL(LEFT JOIN 미매칭) */
    private String parentCategoryCode;
    private long amount;
    /** 적용된 혜택 ID. 혜택 미적용 거래면 NULL */
    private Long appliedBenefitId;
    private long discountAmount;
    private String paymentType;
    /** 무이자할부 여부 원값 'Y'/'N' */
    private String isInterestFree;
    private String paymentStatus;
    private LocalDateTime paymentDate;
    /** 적용 혜택의 통합한도 사용 여부 'Y'/'N' (benefit 조인). 적용 혜택이 없으면 NULL */
    private String useSharedLimit;
    /**
     * 적용 혜택이 실적 제외 대상인지 'Y'/'N' (benefit 조인). 적용 혜택이 없으면 NULL.
     * 가산 때 이 거래를 실적에 안 넣었다면 취소 때도 빼지 않아야 양쪽이 어긋나지 않는다.
     */
    private String excludeFromPerformance;

    public long getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(long expenseId) {
        this.expenseId = expenseId;
    }

    public long getUserCardId() {
        return userCardId;
    }

    public void setUserCardId(long userCardId) {
        this.userCardId = userCardId;
    }

    public long getCardId() {
        return cardId;
    }

    public void setCardId(long cardId) {
        this.cardId = cardId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public void setMerchantCode(String merchantCode) {
        this.merchantCode = merchantCode;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getParentCategoryId() {
        return parentCategoryId;
    }

    public void setParentCategoryId(Long parentCategoryId) {
        this.parentCategoryId = parentCategoryId;
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

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public Long getAppliedBenefitId() {
        return appliedBenefitId;
    }

    public void setAppliedBenefitId(Long appliedBenefitId) {
        this.appliedBenefitId = appliedBenefitId;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(long discountAmount) {
        this.discountAmount = discountAmount;
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

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getUseSharedLimit() {
        return useSharedLimit;
    }

    public void setUseSharedLimit(String useSharedLimit) {
        this.useSharedLimit = useSharedLimit;
    }

    public String getExcludeFromPerformance() {
        return excludeFromPerformance;
    }

    public void setExcludeFromPerformance(String excludeFromPerformance) {
        this.excludeFromPerformance = excludeFromPerformance;
    }
}
