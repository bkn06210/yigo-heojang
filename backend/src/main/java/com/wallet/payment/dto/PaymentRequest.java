package com.wallet.payment.dto;

public class PaymentRequest {

    private Long userCardId;
    private Boolean isRecommendBased;
    private Long categoryId;
    private Long merchantId;
    private String merchantName;
    private Long paymentAmount;
    private String paymentType;
    private String interestFreeYn;

    public Long getUserCardId() { return userCardId; }
    public void setUserCardId(Long userCardId) { this.userCardId = userCardId; }

    public Boolean getIsRecommendBased() { return isRecommendBased; }
    public void setIsRecommendBased(Boolean isRecommendBased) { this.isRecommendBased = isRecommendBased; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public Long getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(Long paymentAmount) { this.paymentAmount = paymentAmount; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getInterestFreeYn() { return interestFreeYn; }
    public void setInterestFreeYn(String interestFreeYn) { this.interestFreeYn = interestFreeYn; }
}
