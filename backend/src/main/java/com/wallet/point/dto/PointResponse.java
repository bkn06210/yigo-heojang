package com.wallet.point.dto;

public class PointResponse {

    private Long pointWalletId;
    private Long pointProviderId;
    private String providerName;
    private String providerType;
    private String logoImage;
    private Long totalPoint;

    public Long getPointWalletId() {
        return pointWalletId;
    }

    public void setPointWalletId(Long pointWalletId) {
        this.pointWalletId = pointWalletId;
    }

    public Long getPointProviderId() {
        return pointProviderId;
    }

    public void setPointProviderId(Long pointProviderId) {
        this.pointProviderId = pointProviderId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getLogoImage() {
        return logoImage;
    }

    public void setLogoImage(String logoImage) {
        this.logoImage = logoImage;
    }

    public Long getTotalPoint() {
        return totalPoint;
    }

    public void setTotalPoint(Long totalPoint) {
        this.totalPoint = totalPoint;
    }
}
