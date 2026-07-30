package com.wallet.engine.dao.dto;

/**
 * 결제 지점 해석 결과의 조회 투영 — 가맹점과 그 카테고리 계층.
 *
 * 추천 요청은 merchantId(또는 categoryId) 하나만 주지만, 매칭에는 id와 코드가 둘 다 필요하다.
 * 혜택 대상은 id로 겨냥하고(benefit.target_category_id) 제외 규칙은 코드로 참조하기 때문이다
 * (benefit_exclusion.exclusion_value = 'CAFE'). 상위 카테고리까지 가져오는 이유는 계층 매칭이다 —
 * 대분류를 겨냥한 혜택이 하위 중분류 결제에 매칭돼야 하고, 계산기는 DB를 다시 조회할 수 없다.
 *
 * categoryId만으로 조회한 경우 가맹점 필드는 null이다. 대분류를 직접 조회하면 상위 필드가 null이다.
 */
public class PaymentTargetRow {

    private Long merchantId;
    private String merchantCode;
    private Long categoryId;
    private String categoryCode;
    private Long parentCategoryId;
    private String parentCategoryCode;

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public void setMerchantCode(String merchantCode) {
        this.merchantCode = merchantCode;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public Long getParentCategoryId() {
        return parentCategoryId;
    }

    public void setParentCategoryId(Long parentCategoryId) {
        this.parentCategoryId = parentCategoryId;
    }

    public String getParentCategoryCode() {
        return parentCategoryCode;
    }

    public void setParentCategoryCode(String parentCategoryCode) {
        this.parentCategoryCode = parentCategoryCode;
    }
}
