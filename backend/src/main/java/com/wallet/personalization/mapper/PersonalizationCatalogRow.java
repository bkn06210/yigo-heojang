package com.wallet.personalization.mapper;

public class PersonalizationCatalogRow {
    private Long parentCategoryId;
    private String parentCategoryCode;
    private String parentCategoryName;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private Boolean selected;
    private Long merchantId;
    private String merchantName;
    private Boolean merchantSelected;
    private Integer priority;

    public Long getParentCategoryId() { return parentCategoryId; }
    public void setParentCategoryId(Long value) { parentCategoryId = value; }
    public String getParentCategoryCode() { return parentCategoryCode; }
    public void setParentCategoryCode(String value) { parentCategoryCode = value; }
    public String getParentCategoryName() { return parentCategoryName; }
    public void setParentCategoryName(String value) { parentCategoryName = value; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long value) { categoryId = value; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String value) { categoryCode = value; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String value) { categoryName = value; }
    public Boolean getSelected() { return selected; }
    public void setSelected(Boolean value) { selected = value; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long value) { merchantId = value; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String value) { merchantName = value; }
    public Boolean getMerchantSelected() { return merchantSelected; }
    public void setMerchantSelected(Boolean value) { merchantSelected = value; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer value) { priority = value; }
}
