package com.wallet.transaction.dto;

public class TransactionDetailResponse {

    private Long expenseId;
    private Long userCardId;
    private String cardName;
    private Long categoryId;
    private String categoryName;
    private Long merchantId;
    private String merchantName;
    private Long paymentAmount;
    private Long appliedBenefitId;
    private String appliedBenefitName;
    private Long discountAmount;
    private String paymentType;
    private String interestFreeYn;
    private String paymentDate;
    private String paymentStatus;
    private String inputType;
    private String createdAt;

    public Long getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(Long expenseId) {
        this.expenseId = expenseId;
    }

    public Long getUserCardId() {
        return userCardId;
    }

    public void setUserCardId(Long userCardId) {
        this.userCardId = userCardId;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public Long getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(Long paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public Long getAppliedBenefitId() {
        return appliedBenefitId;
    }

    public void setAppliedBenefitId(Long appliedBenefitId) {
        this.appliedBenefitId = appliedBenefitId;
    }

    public String getAppliedBenefitName() {
        return appliedBenefitName;
    }

    public void setAppliedBenefitName(String appliedBenefitName) {
        this.appliedBenefitName = appliedBenefitName;
    }

    public Long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getInterestFreeYn() {
        return interestFreeYn;
    }

    public void setInterestFreeYn(String interestFreeYn) {
        this.interestFreeYn = interestFreeYn;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
