package com.wallet.payment.dto;

public class PaymentCommand {

    private Long paymentId;
    private Long userId;
    private Long userCardId;
    private Long expenseId;
    private Long categoryId;
    private Long merchantId;
    private String merchantName;
    private Long paymentAmount;
    private String paymentStatus;
    private String paymentChannel;
    private String isRecommendBased;
    private String paymentType;
    private String interestFreeYn;

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getUserCardId() { return userCardId; }
    public void setUserCardId(Long userCardId) { this.userCardId = userCardId; }

    public Long getExpenseId() { return expenseId; }
    public void setExpenseId(Long expenseId) { this.expenseId = expenseId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public Long getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(Long paymentAmount) { this.paymentAmount = paymentAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentChannel() { return paymentChannel; }
    public void setPaymentChannel(String paymentChannel) { this.paymentChannel = paymentChannel; }

    public String getIsRecommendBased() { return isRecommendBased; }
    public void setIsRecommendBased(String isRecommendBased) { this.isRecommendBased = isRecommendBased; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getInterestFreeYn() { return interestFreeYn; }
    public void setInterestFreeYn(String interestFreeYn) { this.interestFreeYn = interestFreeYn; }
}
