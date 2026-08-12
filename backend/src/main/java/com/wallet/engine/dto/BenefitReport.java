package com.wallet.engine.dto;

import java.util.List;

/**
 * 한 달 혜택 리포트.
 *
 * 요약(총액·최대 부문)과 부문별 목록, 부문 안의 거래까지 한 번에 담는다.
 * 화면이 요약 → 목록 → 상세 세 단계로 파고드는데, 단계마다 다시 조회하면 그 사이에
 * 결제가 일어났을 때 합계와 상세가 어긋난다. 한 번에 만들면 세 화면이 같은 값을 본다.
 *
 * 담기는 거래는 실제로 혜택을 받은 건뿐이라 한 달치라도 목록이 길지 않다.
 */
public class BenefitReport {

    private String yearMonth;

    /** 이번 달 받은 혜택 총액(원). */
    private long totalBenefitAmount;

    /** 가장 많이 받은 부문. 받은 혜택이 없으면 null. */
    private Long topCategoryId;
    private String topCategoryName;
    private Long topCategoryBenefitAmount;

    /** 부문별 혜택 합계. 금액 내림차순, 동점이면 categoryId 오름차순. */
    private List<BenefitReportCategory> categories;

    public BenefitReport(String yearMonth, long totalBenefitAmount, Long topCategoryId,
                         String topCategoryName, Long topCategoryBenefitAmount,
                         List<BenefitReportCategory> categories) {
        this.yearMonth = yearMonth;
        this.totalBenefitAmount = totalBenefitAmount;
        this.topCategoryId = topCategoryId;
        this.topCategoryName = topCategoryName;
        this.topCategoryBenefitAmount = topCategoryBenefitAmount;
        this.categories = categories;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public long getTotalBenefitAmount() {
        return totalBenefitAmount;
    }

    public Long getTopCategoryId() {
        return topCategoryId;
    }

    public String getTopCategoryName() {
        return topCategoryName;
    }

    public Long getTopCategoryBenefitAmount() {
        return topCategoryBenefitAmount;
    }

    public List<BenefitReportCategory> getCategories() {
        return categories;
    }
}
