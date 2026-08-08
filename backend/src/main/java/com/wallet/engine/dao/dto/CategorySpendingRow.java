package com.wallet.engine.dao.dto;

/**
 * 이번 달 업종별 소비 집계 한 줄 — 홈 브리핑 판정용.
 *
 * <b>혜택을 받았는지와 무관하게 결제 전부를 센다.</b> 혜택 리포트(받은 혜택)와 목적이 반대다 —
 * 이쪽이 찾는 것은 "결제는 했는데 혜택은 못 받은" 자리라, 받은 것만 보면 그 자리가 안 보인다.
 *
 * 상위 분류를 함께 담는 이유는 혜택 대상과 맞춰봐야 하기 때문이다. 혜택이 대분류(외식)를
 * 겨냥하면 하위 중분류(카페) 결제도 대상인데, 중분류 id만 있으면 그 매칭이 안 된다.
 */
public class CategorySpendingRow {

    private Long categoryId;
    private String categoryName;
    /** 대분류 거래는 상위가 없어 null이다. */
    private Long parentCategoryId;
    private int paymentCount;
    private long totalAmount;

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

    public Long getParentCategoryId() {
        return parentCategoryId;
    }

    public void setParentCategoryId(Long parentCategoryId) {
        this.parentCategoryId = parentCategoryId;
    }

    public int getPaymentCount() {
        return paymentCount;
    }

    public void setPaymentCount(int paymentCount) {
        this.paymentCount = paymentCount;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }
}
