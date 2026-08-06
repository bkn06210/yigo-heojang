package com.wallet.engine.dto;

import java.util.List;

/** 부문 하나의 혜택 합계와 그 안의 거래들. */
public class BenefitReportCategory {

    private long categoryId;
    private String categoryName;

    /** 이 부문에서 받은 혜택 합계(원). */
    private long benefitAmount;

    private List<BenefitReportDetail> details;

    public BenefitReportCategory(long categoryId, String categoryName, long benefitAmount,
                                 List<BenefitReportDetail> details) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.benefitAmount = benefitAmount;
        this.details = details;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public long getBenefitAmount() {
        return benefitAmount;
    }

    public List<BenefitReportDetail> getDetails() {
        return details;
    }
}
