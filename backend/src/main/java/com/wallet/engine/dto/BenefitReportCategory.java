package com.wallet.engine.dto;

import java.util.List;

/** 부문 하나의 혜택 합계와 그 안의 거래들. */
public class BenefitReportCategory {

    private long categoryId;
    private String categoryName;

    /** 상위 분류명. 화면이 "외식 > 카페"로 보여줄 수 있다. 대분류면 null. */
    private String parentCategoryName;

    /** 이 부문에서 받은 혜택 합계(원). */
    private long benefitAmount;

    private List<BenefitReportDetail> details;

    public BenefitReportCategory(long categoryId, String categoryName, String parentCategoryName,
                                 long benefitAmount, List<BenefitReportDetail> details) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.parentCategoryName = parentCategoryName;
        this.benefitAmount = benefitAmount;
        this.details = details;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getParentCategoryName() {
        return parentCategoryName;
    }

    public long getBenefitAmount() {
        return benefitAmount;
    }

    public List<BenefitReportDetail> getDetails() {
        return details;
    }
}
