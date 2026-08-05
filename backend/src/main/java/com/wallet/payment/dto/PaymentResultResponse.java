package com.wallet.payment.dto;

public class PaymentResultResponse {

    private Long paymentId;
    private Long expenseId;
    private String cardName;
    private String merchantName;
    private Long paymentAmount;
    private String paymentStatus;
    private Long savedPoint;
    private Long totalPoint;
    private String completedAt;

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Long getExpenseId() { return expenseId; }
    public void setExpenseId(Long expenseId) { this.expenseId = expenseId; }

    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public Long getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(Long paymentAmount) { this.paymentAmount = paymentAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public Long getSavedPoint() { return savedPoint; }
    public void setSavedPoint(Long savedPoint) { this.savedPoint = savedPoint; }

    public Long getTotalPoint() { return totalPoint; }
    public void setTotalPoint(Long totalPoint) { this.totalPoint = totalPoint; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
}
