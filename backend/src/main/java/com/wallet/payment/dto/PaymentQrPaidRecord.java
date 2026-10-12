package com.wallet.payment.dto;

/**
 * 이미 결제에 쓰인 QR의 결제 내용 — 같은 QR 재요청이 재시도인지 다른 결제인지 가르고, 재시도면 처음 결과를 돌려준다.
 */
public class PaymentQrPaidRecord {

    private Long paymentId;
    private Long expenseId;
    private Long merchantId;
    private String merchantName;
    private Long categoryId;

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Long getExpenseId() { return expenseId; }
    public void setExpenseId(Long expenseId) { this.expenseId = expenseId; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
