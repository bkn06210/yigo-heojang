package com.wallet.membership.dto;

public class MembershipUsagePlaceResponse {

    private Long usagePlaceId;
    private String placeName;
    private String categoryName;
    private String useYn;
    private String description;

    public Long getUsagePlaceId() {
        return usagePlaceId;
    }

    public void setUsagePlaceId(Long usagePlaceId) {
        this.usagePlaceId = usagePlaceId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
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