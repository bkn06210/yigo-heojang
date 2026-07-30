package com.wallet.membership.dto;

public class MembershipProviderResponse {

    private Long pointProviderId;
    private String providerName;
    private String providerType;
    private String logoImageUrl;
    private Boolean isRegistered;
    private String defaultRecommendYn;
    private Integer recommendPriority;
    private String recommendMessage;

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

    public Boolean getIsRegistered() {
        return isRegistered;
    }

    public void setIsRegistered(Boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    public String getDefaultRecommendYn() {
        return defaultRecommendYn;
    }

    public void setDefaultRecommendYn(String defaultRecommendYn) {
        this.defaultRecommendYn = defaultRecommendYn;
    }

    public Integer getRecommendPriority() {
        return recommendPriority;
    }

    public void setRecommendPriority(Integer recommendPriority) {
        this.recommendPriority = recommendPriority;
    }

    public String getRecommendMessage() {
        return recommendMessage;
    }

    public void setRecommendMessage(String recommendMessage) {
        this.recommendMessage = recommendMessage;
    }
}