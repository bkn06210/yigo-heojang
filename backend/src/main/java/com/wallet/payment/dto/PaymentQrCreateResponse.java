package com.wallet.payment.dto;

public class PaymentQrCreateResponse {

    private String qrToken;
    private String qrImageBase64;
    private String expiresAt;

    public PaymentQrCreateResponse(String qrToken, String qrImageBase64, String expiresAt) {
        this.qrToken = qrToken;
        this.qrImageBase64 = qrImageBase64;
        this.expiresAt = expiresAt;
    }

    public String getQrToken() {
        return qrToken;
    }

    public String getQrImageBase64() {
        return qrImageBase64;
    }

    public String getExpiresAt() {
        return expiresAt;
    }
}