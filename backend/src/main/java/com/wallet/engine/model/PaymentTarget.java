package com.wallet.engine.model;

/**
 * 결제가 일어나는 지점 — 혜택 매칭의 기준.
 *
 * 카테고리는 결제 카테고리와 그 상위 카테고리를 함께 담는다. 혜택이 대분류를 겨냥하면
 * 하위 중분류 결제까지 매칭해야 하는데(계층 2단계 고정), 계산기가 DB를 다시 조회할 수 없기 때문이다.
 *
 * id와 code를 둘 다 갖는 이유: 대상 매칭은 id로(benefit.target_category_id),
 * 제외 판정은 코드 문자열로(benefit_exclusion.exclusion_value = 'CAFE') 이뤄진다.
 * 스키마가 두 방식을 섞어 쓰므로 둘 다 필요하다.
 *
 * 전부 null이면 "장소 미정" — 전 가맹점(ALL) 혜택만 매칭된다.
 */
public final class PaymentTarget {

    private final Long merchantId;
    private final String merchantCode;
    private final Long categoryId;
    private final String categoryCode;
    private final Long parentCategoryId;
    private final String parentCategoryCode;

    private PaymentTarget(Builder builder) {
        this.merchantId = builder.merchantId;
        this.merchantCode = builder.merchantCode;
        this.categoryId = builder.categoryId;
        this.categoryCode = builder.categoryCode;
        this.parentCategoryId = builder.parentCategoryId;
        this.parentCategoryCode = builder.parentCategoryCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 장소 미정 — 전 가맹점 혜택만 대상이 된다 */
    public static PaymentTarget unspecified() {
        return builder().build();
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public Long getParentCategoryId() {
        return parentCategoryId;
    }

    public String getParentCategoryCode() {
        return parentCategoryCode;
    }

    public static final class Builder {
        private Long merchantId;
        private String merchantCode;
        private Long categoryId;
        private String categoryCode;
        private Long parentCategoryId;
        private String parentCategoryCode;

        private Builder() {
        }

        public Builder merchantId(Long merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public Builder merchantCode(String merchantCode) {
            this.merchantCode = merchantCode;
            return this;
        }

        public Builder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder categoryCode(String categoryCode) {
            this.categoryCode = categoryCode;
            return this;
        }

        public Builder parentCategoryId(Long parentCategoryId) {
            this.parentCategoryId = parentCategoryId;
            return this;
        }

        public Builder parentCategoryCode(String parentCategoryCode) {
            this.parentCategoryCode = parentCategoryCode;
            return this;
        }

        public PaymentTarget build() {
            return new PaymentTarget(this);
        }
    }
}
