package com.wallet.membership.dto;

import java.util.List;

public class MembershipDetailResponse {
    
    private String officialSiteUrl;
    private Long membershipRegisterId;
    private Long pointProviderId;
    private String providerName;
    private String logoImage;
    private String registerStatus;
    private Long totalPoint;
    private String registeredAt;
    private List<MembershipUsagePlaceResponse> usagePlaces;
    public String getOfficialSiteUrl() {
        return officialSiteUrl;
    }

    public void setOfficialSiteUrl(String officialSiteUrl) {
        this.officialSiteUrl = officialSiteUrl;
    }

    public Long getMembershipRegisterId() {
        return membershipRegisterId;
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

    public String getLogoImage() {
        return logoImage;
    }

    public void setLogoImage(String logoImage) {
        this.logoImage = logoImage;
    }

    public String getRegisterStatus() {
        return registerStatus;
    }

    public void setRegisterStatus(String registerStatus) {
        this.registerStatus = registerStatus;
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

    public List<MembershipUsagePlaceResponse> getUsagePlaces() {
        return usagePlaces;
    }

    public void setUsagePlaces(List<MembershipUsagePlaceResponse> usagePlaces) {
        this.usagePlaces = usagePlaces;
    }
}
