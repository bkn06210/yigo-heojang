package com.wallet.membership.dto;

public class MyMembershipResponse {

    private Long membershipRegisterId;
    private Long pointProviderId;
    private String providerName;
    private String providerType;
    private String logoImageUrl;
    private Long totalPoint;
    private String registeredAt;
    private String officialSiteUrl;
    public Long getMembershipRegisterId() {
        return membershipRegisterId;
    }
    public String getOfficialSiteUrl() {
        return officialSiteUrl;
    }

    public void setOfficialSiteUrl(String officialSiteUrl) {
        this.officialSiteUrl = officialSiteUrl;
    }
    public void setMembershipRegisterId(Long membershipRegisterId) {
        this.membershipRegisterId = membershipRegisterId;
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

    public String getLogoImageUrl() {
        return logoImageUrl;
    }

    public void setLogoImageUrl(String logoImageUrl) {
        this.logoImageUrl = logoImageUrl;
    }

    public Long getTotalPoint() {
        return totalPoint;
    }

    public void setTotalPoint(Long totalPoint) {
        this.totalPoint = totalPoint;
    }

    public String getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(String registeredAt) {
        this.registeredAt = registeredAt;
    }
}