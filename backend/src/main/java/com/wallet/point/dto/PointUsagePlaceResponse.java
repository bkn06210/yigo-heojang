package com.wallet.point.dto;

public class PointUsagePlaceResponse {

    private Long usagePlaceId;
    private Long pointProviderId;
    private String providerName;
    private String placeName;
    private Long categoryId;
    private String categoryName;
    private String useYn;
    private String description;

    public Long getUsagePlaceId() {
        return usagePlaceId;
    }

    public void setUsagePlaceId(Long usagePlaceId) {
        this.usagePlaceId = usagePlaceId;
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

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
