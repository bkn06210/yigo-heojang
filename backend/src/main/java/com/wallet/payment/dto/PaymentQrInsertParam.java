package com.wallet.payment.dto;

import java.sql.Timestamp;

public class PaymentQrInsertParam {

    private String qrToken;
    private Long memberId;
    private Long userCardId;
    private String status;
    private Timestamp expiresAt;

    public PaymentQrInsertParam(String qrToken, Long memberId, Long userCardId, String status, Timestamp expiresAt) {
        this.qrToken = qrToken;
        this.memberId = memberId;
        this.userCardId = userCardId;
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

    public String getStatus() {
        return status;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }
}
