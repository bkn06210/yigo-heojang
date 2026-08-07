package com.wallet.payment.dto;

public class PaymentQrCreateRequest {

    private Long userCardId;
    private Long paymentAmount;

    public Long getUserCardId() {
        return userCardId;
    }

    public void setUserCardId(Long userCardId) {
        this.userCardId = userCardId;
    }

    public Long getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(Long paymentAmount) { this.paymentAmount = paymentAmount; }
}
