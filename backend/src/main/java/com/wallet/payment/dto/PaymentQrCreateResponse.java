package com.wallet.payment.dto;

public class PaymentQrCreateResponse {

    private String qrToken;
    private String expiresAt;

    public PaymentQrCreateResponse(String qrToken, String expiresAt) {
        this.qrToken = qrToken;
        this.expiresAt = expiresAt;
    }

    public String getQrToken() {
        return qrToken;
    }

    public String getExpiresAt() {
        return expiresAt;
    }
}
