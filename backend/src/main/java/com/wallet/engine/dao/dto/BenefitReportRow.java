package com.wallet.engine.dao.dto;

import java.time.LocalDateTime;

/**
 * 혜택을 받은 거래 한 건. 리포트 집계의 원본 행이다.
 *
 * 집계를 SQL에서 하지 않고 행으로 받아 자바에서 묶는 이유는, 같은 조회로 총액·부문별 합계·
 * 거래 목록 셋을 모두 만들기 때문이다. 각각 SQL을 따로 두면 세 값이 서로 어긋날 수 있다.
 */
public class BenefitReportRow {

    private long expenseId;
    private long amount;
    private long discountAmount;
    private LocalDateTime paymentDate;
    private String merchantName;

    /** 묶음 단위 카테고리. 거래에 기록된 값 그대로다(대개 중분류). */
    private long groupCategoryId;
    private String groupCategoryName;

    /** 상위 분류명. 화면이 "외식 > 카페"로 보여줄 수 있게 함께 내려준다. 대분류 거래면 null. */
    private String parentCategoryName;

    private String benefitName;
    private String cardName;

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

    public long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public long getGroupCategoryId() {
        return groupCategoryId;
    }

    public void setGroupCategoryId(long groupCategoryId) {
        this.groupCategoryId = groupCategoryId;
    }

    public String getGroupCategoryName() {
        return groupCategoryName;
    }

    public void setGroupCategoryName(String groupCategoryName) {
        this.groupCategoryName = groupCategoryName;
    }

    public String getParentCategoryName() {
        return parentCategoryName;
    }

    public void setParentCategoryName(String parentCategoryName) {
        this.parentCategoryName = parentCategoryName;
    }

    public String getBenefitName() {
        return benefitName;
    }

    public void setBenefitName(String benefitName) {
        this.benefitName = benefitName;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }
}
