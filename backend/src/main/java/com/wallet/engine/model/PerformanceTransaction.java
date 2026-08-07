package com.wallet.engine.model;

/**
 * 전월실적 판정 대상 거래 한 건 (expense의 투영).
 *
 * expense 전체가 아니라 실적 판정에 필요한 컬럼만 담는다. merchantId를 안 넣는 게 중요하다 —
 * performance_exclusion에는 MERCHANT 유형이 없다(benefit_exclusion에만 있다). 넣으면
 * "언젠가 쓰겠지" 필드가 되고 스키마와 어긋난 인상을 준다.
 *
 * discountAmount는 boolean으로 미리 접지 않고 원값으로 받는다 — "discount_amount > 0이면
 * 할인받은 거래"라는 것은 엔진의 판정 규칙이지 DB 매핑 규칙이 아니다. 엔진 안에 있어야
 * 테스트로 고정된다. 반대로 'Y'/'N'·payment_status 문자열 → boolean 변환은 DAO 경계에서 한다.
 *
 * 카테고리는 결제 카테고리 코드와 상위 카테고리 코드를 함께 담는다 — 대분류를 제외하는 규칙이
 * 하위 중분류 결제까지 걸러야 하는데(계층 2단계 고정) 계산기가 DB를 다시 조회할 수 없기 때문이다.
 */
public final class PerformanceTransaction {

    private final long expenseId;
    private final long amount;
    private final boolean canceled;
    private final String categoryCode;
    private final String parentCategoryCode;
    private final String paymentType;
    private final boolean interestFree;
    private final long discountAmount;
    /**
     * 이 거래에 적용된 혜택이 실적 제외 대상인가 (benefit.exclude_from_performance).
     *
     * 카드 단위 제외 규칙(performance_exclusion)과 축이 다르다. TRANSACTION_ATTR='DISCOUNTED'로
     * 적으면 <b>다른 혜택을 받은 거래까지</b> 빠져 실적이 실제보다 낮아지는데, 이 플래그는
     * 지정된 그 혜택을 받은 거래만 뺀다. 적용 혜택이 없으면 false다.
     */
    private final boolean benefitExcludedFromPerformance;

    private PerformanceTransaction(Builder builder) {
        if (builder.amount < 0) {
            throw new IllegalArgumentException("거래 금액은 음수일 수 없다: " + builder.amount);
        }
        if (builder.discountAmount < 0) {
            throw new IllegalArgumentException("할인액은 음수일 수 없다: " + builder.discountAmount);
        }
        this.expenseId = builder.expenseId;
        this.amount = builder.amount;
        this.canceled = builder.canceled;
        this.categoryCode = builder.categoryCode;
        this.parentCategoryCode = builder.parentCategoryCode;
        this.paymentType = builder.paymentType;
        this.interestFree = builder.interestFree;
        this.discountAmount = builder.discountAmount;
        this.benefitExcludedFromPerformance = builder.benefitExcludedFromPerformance;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getExpenseId() {
        return expenseId;
    }

    public long getAmount() {
        return amount;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getParentCategoryCode() {
        return parentCategoryCode;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public boolean isInterestFree() {
        return interestFree;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public boolean isBenefitExcludedFromPerformance() {
        return benefitExcludedFromPerformance;
    }

    public static final class Builder {
        private long expenseId;
        private long amount;
        private boolean canceled;
        private String categoryCode;
        private String parentCategoryCode;
        private String paymentType;
        private boolean interestFree;
        private long discountAmount;
        private boolean benefitExcludedFromPerformance;

        private Builder() {
        }

        public Builder expenseId(long expenseId) {
            this.expenseId = expenseId;
            return this;
        }

        public Builder amount(long amount) {
            this.amount = amount;
            return this;
        }

        public Builder canceled(boolean canceled) {
            this.canceled = canceled;
            return this;
        }

        public Builder categoryCode(String categoryCode) {
            this.categoryCode = categoryCode;
            return this;
        }

        public Builder parentCategoryCode(String parentCategoryCode) {
            this.parentCategoryCode = parentCategoryCode;
            return this;
        }

        public Builder paymentType(String paymentType) {
            this.paymentType = paymentType;
            return this;
        }

        public Builder interestFree(boolean interestFree) {
            this.interestFree = interestFree;
            return this;
        }

        public Builder discountAmount(long discountAmount) {
            this.discountAmount = discountAmount;
            return this;
        }

        public Builder benefitExcludedFromPerformance(boolean benefitExcludedFromPerformance) {
            this.benefitExcludedFromPerformance = benefitExcludedFromPerformance;
            return this;
        }

        public PerformanceTransaction build() {
            return new PerformanceTransaction(this);
        }
    }
}
