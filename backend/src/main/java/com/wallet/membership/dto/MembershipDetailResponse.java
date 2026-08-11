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
    /** 제공사 한 줄 소개 (예: 이랜드 계열 제휴 멤버십) */
    private String recommendMessage;
    /** 최근 3개월간 이 멤버십 사용처에서 결제한 건수 */
    private Long recentUseCount;
    /** 최근 3개월간 이 멤버십 사용처에서 결제한 금액(원) */
    private Long recentUseAmount;
    private List<MembershipUsagePlaceResponse> usagePlaces;

    public String getRecommendMessage() {
        return recommendMessage;
    }

    public void setRecommendMessage(String recommendMessage) {
        this.recommendMessage = recommendMessage;
    }

    public Long getRecentUseCount() {
        return recentUseCount;
    }

    public void setRecentUseCount(Long recentUseCount) {
        this.recentUseCount = recentUseCount;
    }

    public Long getRecentUseAmount() {
        return recentUseAmount;
    }

    public void setRecentUseAmount(Long recentUseAmount) {
        this.recentUseAmount = recentUseAmount;
    }
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
