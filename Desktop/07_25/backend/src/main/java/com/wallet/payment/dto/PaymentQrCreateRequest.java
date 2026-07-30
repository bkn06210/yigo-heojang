package com.wallet.payment.dto;

public class PaymentQrCreateRequest {

    private Long userCardId;

    public Long getUserCardId() {
        return userCardId;
    }

    public void setUserCardId(Long userCardId) {
        this.userCardId = userCardId;
    }
}