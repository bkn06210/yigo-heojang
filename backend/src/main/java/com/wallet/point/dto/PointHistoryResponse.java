package com.wallet.point.dto;

public class PointHistoryResponse {

    private Long pointHistoryId;
    private Long pointWalletId;
    private String providerName;
    private String pointType;
    private Long pointAmount;
    private String content;
    private String occurredAt;

    public Long getPointHistoryId() {
        return pointHistoryId;
    }

    public void setPointHistoryId(Long pointHistoryId) {
        this.pointHistoryId = pointHistoryId;
    }

    public Long getPointWalletId() {
        return pointWalletId;
    }

    public void setPointWalletId(Long pointWalletId) {
        this.pointWalletId = pointWalletId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getPointType() {
        return pointType;
    }

    public void setPointType(String pointType) {
        this.pointType = pointType;
    }

    public Long getPointAmount() {
        return pointAmount;
    }

    public void setPointAmount(Long pointAmount) {
        this.pointAmount = pointAmount;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }
}
