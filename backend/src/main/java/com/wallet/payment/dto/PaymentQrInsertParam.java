package com.wallet.payment.dto;

import java.sql.Timestamp;

public class PaymentQrInsertParam {

    private String qrToken;
    private Long memberId;
    private Long userCardId;
    private Long paymentAmount;
    private String status;
    private Timestamp expiresAt;

    public PaymentQrInsertParam(String qrToken, Long memberId, Long userCardId, Long paymentAmount, String status, Timestamp expiresAt) {
        this.qrToken = qrToken;
        this.memberId = memberId;
        this.userCardId = userCardId;
        this.paymentAmount = paymentAmount;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public String getQrToken() {
        return qrToken;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getUserCardId() {
        return userCardId;
    }

    public Long getPaymentAmount() { return paymentAmount; }

    public String getStatus() {
        return status;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }
}
