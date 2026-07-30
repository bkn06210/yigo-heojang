package com.wallet.payment.dto;

public class PaymentProcessResponse {

    private Long paymentId;
    private Long expenseId;
    private String paymentStatus;
    private String paymentChannel;
    private Long appliedBenefitId;
    private String appliedBenefitName;
    private Long discountAmount;
    private Long savedPoint;
    private Long totalPoint;
    private String completedAt;

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Long getExpenseId() { return expenseId; }
    public void setExpenseId(Long expenseId) { this.expenseId = expenseId; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentChannel() { return paymentChannel; }
    public void setPaymentChannel(String paymentChannel) { this.paymentChannel = paymentChannel; }

    public Long getAppliedBenefitId() { return appliedBenefitId; }
    public void setAppliedBenefitId(Long appliedBenefitId) { this.appliedBenefitId = appliedBenefitId; }

    public String getAppliedBenefitName() { return appliedBenefitName; }
    public void setAppliedBenefitName(String appliedBenefitName) { this.appliedBenefitName = appliedBenefitName; }

    public Long getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Long discountAmount) { this.discountAmount = discountAmount; }

    public Long getSavedPoint() { return savedPoint; }
    public void setSavedPoint(Long savedPoint) { this.savedPoint = savedPoint; }

    public Long getTotalPoint() { return totalPoint; }
    public void setTotalPoint(Long totalPoint) { this.totalPoint = totalPoint; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
}