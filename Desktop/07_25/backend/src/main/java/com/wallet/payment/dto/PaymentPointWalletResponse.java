package com.wallet.payment.dto;

public class PaymentPointWalletResponse {

    private Long pointWalletId;
    private Long pointProviderId;
    private String providerName;
    private Long totalPoint;

    public Long getPointWalletId() { return pointWalletId; }
    public void setPointWalletId(Long pointWalletId) { this.pointWalletId = pointWalletId; }

    public Long getPointProviderId() { return pointProviderId; }
    public void setPointProviderId(Long pointProviderId) { this.pointProviderId = pointProviderId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public Long getTotalPoint() { return totalPoint; }
    public void setTotalPoint(Long totalPoint) { this.totalPoint = totalPoint; }
}