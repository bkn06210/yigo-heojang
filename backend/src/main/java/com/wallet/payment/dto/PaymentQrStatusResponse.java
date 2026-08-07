package com.wallet.payment.dto;

public class PaymentQrStatusResponse {
    private final String qrToken;
    private final String status;
    private final String expiresAt;
    private final Long paymentId;

    public PaymentQrStatusResponse(String qrToken, String status, String expiresAt, Long paymentId) {
        this.qrToken = qrToken;
        this.status = status;
        this.expiresAt = expiresAt;
        this.paymentId = paymentId;
    }

    public String getQrToken() { return qrToken; }
    public String getStatus() { return status; }
    public String getExpiresAt() { return expiresAt; }
    public Long getPaymentId() { return paymentId; }
}
