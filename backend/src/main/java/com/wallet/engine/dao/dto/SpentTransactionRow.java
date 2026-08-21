package com.wallet.engine.dao.dto;

import java.time.LocalDateTime;

/**
 * 지난달 결제 한 건 — 카드 추천 시뮬레이션에 흘려보낼 입력.
 *
 * 집계(CategorySpendingRow)와 달리 <b>거래 단위</b>다. 건당 최대·횟수·일 한도가 결제 건수에 따라
 * 걸리므로 총액만으로는 혜택을 구할 수 없다.
 *
 * 가맹점·카테고리 코드를 id와 함께 내리는 이유는 혜택의 제외 규칙이 코드 문자열로 적혀 있기
 * 때문이다. 상위 카테고리는 LEFT JOIN이라 대분류 거래에서는 비어 있다.
 */
public class SpentTransactionRow {

    private LocalDateTime paymentDate;
    private Long merchantId;
    private String merchantCode;
    private Long categoryId;
    private String categoryCode;
    private Long parentCategoryId;
    private String parentCategoryCode;
    private long amount;
    private String paymentType;

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public Long getParentCategoryId() {
        return parentCategoryId;
    }

    public void setParentCategoryId(Long parentCategoryId) {
        this.parentCategoryId = parentCategoryId;
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

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }
}
