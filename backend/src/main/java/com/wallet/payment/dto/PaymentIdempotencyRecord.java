package com.wallet.payment.dto;

/**
 * 이미 쓰인 멱등 키와 그 키로 처리된 결제 — 재요청에 처음 결과를 돌려주는 데 쓴다.
 */
public class PaymentIdempotencyRecord {

    private String requestHash;
    private Long paymentId;
    private Long expenseId;
    private Long appliedBenefitId;
    private Long discountAmount;

    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Long getExpenseId() { return expenseId; }
    public void setExpenseId(Long expenseId) { this.expenseId = expenseId; }
    public Long getAppliedBenefitId() { return appliedBenefitId; }
    public void setAppliedBenefitId(Long appliedBenefitId) { this.appliedBenefitId = appliedBenefitId; }
    public Long getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Long discountAmount) { this.discountAmount = discountAmount; }
}
