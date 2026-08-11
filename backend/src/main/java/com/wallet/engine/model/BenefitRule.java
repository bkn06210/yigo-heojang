package com.wallet.engine.model;

import java.math.BigDecimal;

/**
 * 혜택 규칙 — benefit 테이블 한 행의 계산용 표현.
 *
 * 구간별 한도 상속(benefit_tier_limit)이 이미 반영된 "유효값"이다.
 * DB에서 읽은 원값은 TierLimitResolver를 거쳐 이 객체가 된다 — 계산기는 상속을 모른다.
 *
 * NULL 규약: 상한·조건 필드(Long/Integer)의 null은 "제약 없음", 0은 "혜택 없음/항상 미달".
 * 이 구분을 뭉개면 혜택이 통째로 사라지므로 원시 타입(long/int)으로 받지 않는다.
 *
 * 기간 상한은 일·월·분기·연 × 금액·횟수 여덟 축이 각각 독립이다. 좁은 기간의 한도를 넓은 기간
 * 필드에 넣거나 환산하면 리셋 주기가 달라져 금액만 조용히 틀린다(분기 한도를 월에 넣으면 세 배).
 *
 * limit_group_code는 여기 없다 — 묶음 한도의 그룹 합산은 호출자(카드 단위 계산) 책임이고,
 * 계산기는 CalcContext.monthlyUsedAmount에 합산된 값이 들어온다는 계약만 안다.
 */
public final class BenefitRule {

    private final long benefitId;
    private final BenefitKind benefitKind;
    private final CalcMethod calcMethod;
    /** RATE면 퍼센트(예: 10.50 = 10.5%), FIXED·COUNT_STEP이면 원 단위 금액. DECIMAL(10,2) — double 금지 */
    private final BigDecimal benefitValue;
    /** COUNT_STEP에서 몇 회마다 지급하는지. 다른 계산 방식이면 null */
    private final Integer stepCount;
    private final boolean requirePerformance;
    /** 결제수단 조건(예: SIMPLE_PAY). null이면 수단 무관 */
    private final String requirePaymentType;
    private final Long minTxnAmount;
    private final Long maxEligibleAmount;
    private final Long maxBenefitPerTxn;
    private final Long monthlyLimit;
    private final Long dailyLimit;
    private final Long quarterlyLimit;
    private final Long yearlyLimit;
    private final Integer monthlyCountLimit;
    private final Integer dailyCountLimit;
    private final Integer quarterlyCountLimit;
    private final Integer yearlyCountLimit;
    private final boolean useSharedLimit;

    private BenefitRule(Builder builder) {
        if (builder.benefitKind == null) {
            throw new IllegalArgumentException("benefitKind는 필수다");
        }
        if (builder.calcMethod == null) {
            throw new IllegalArgumentException("calcMethod는 필수다");
        }
        if (builder.benefitValue == null) {
            throw new IllegalArgumentException("benefitValue는 필수다 (GIFT는 0)");
        }
        // stepCount 없이 COUNT_STEP을 만들면 "몇 회마다"를 모른 채 계산하게 된다.
        // 0으로 뭉개면 나눗셈이 터지고, 1로 뭉개면 매 결제 지급이라 실제의 N배가 나간다.
        if (builder.calcMethod == CalcMethod.COUNT_STEP
                && (builder.stepCount == null || builder.stepCount <= 0)) {
            throw new IllegalArgumentException("COUNT_STEP은 stepCount가 1 이상이어야 한다: " + builder.stepCount);
        }
        this.benefitId = builder.benefitId;
        this.benefitKind = builder.benefitKind;
        this.calcMethod = builder.calcMethod;
        this.benefitValue = builder.benefitValue;
        this.stepCount = builder.stepCount;
        this.requirePerformance = builder.requirePerformance;
        this.requirePaymentType = builder.requirePaymentType;
        this.minTxnAmount = builder.minTxnAmount;
        this.maxEligibleAmount = builder.maxEligibleAmount;
        this.maxBenefitPerTxn = builder.maxBenefitPerTxn;
        this.monthlyLimit = builder.monthlyLimit;
        this.dailyLimit = builder.dailyLimit;
        this.quarterlyLimit = builder.quarterlyLimit;
        this.yearlyLimit = builder.yearlyLimit;
        this.monthlyCountLimit = builder.monthlyCountLimit;
        this.dailyCountLimit = builder.dailyCountLimit;
        this.quarterlyCountLimit = builder.quarterlyCountLimit;
        this.yearlyCountLimit = builder.yearlyCountLimit;
        this.useSharedLimit = builder.useSharedLimit;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 현재 값을 복사한 빌더 — TierLimitResolver가 구간별 유효값으로 갈아끼울 때 사용 */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.benefitId = this.benefitId;
        builder.benefitKind = this.benefitKind;
        builder.calcMethod = this.calcMethod;
        builder.benefitValue = this.benefitValue;
        builder.stepCount = this.stepCount;
        builder.requirePerformance = this.requirePerformance;
        builder.requirePaymentType = this.requirePaymentType;
        builder.minTxnAmount = this.minTxnAmount;
        builder.maxEligibleAmount = this.maxEligibleAmount;
        builder.maxBenefitPerTxn = this.maxBenefitPerTxn;
        builder.monthlyLimit = this.monthlyLimit;
        builder.dailyLimit = this.dailyLimit;
        builder.quarterlyLimit = this.quarterlyLimit;
        builder.yearlyLimit = this.yearlyLimit;
        builder.monthlyCountLimit = this.monthlyCountLimit;
        builder.dailyCountLimit = this.dailyCountLimit;
        builder.quarterlyCountLimit = this.quarterlyCountLimit;
        builder.yearlyCountLimit = this.yearlyCountLimit;
        builder.useSharedLimit = this.useSharedLimit;
        return builder;
    }

    public long getBenefitId() {
        return benefitId;
    }

    public BenefitKind getBenefitKind() {
        return benefitKind;
    }

    public CalcMethod getCalcMethod() {
        return calcMethod;
    }

    public BigDecimal getBenefitValue() {
        return benefitValue;
    }

    public Integer getStepCount() {
        return stepCount;
    }

    public boolean isRequirePerformance() {
        return requirePerformance;
    }

    public String getRequirePaymentType() {
        return requirePaymentType;
    }

    public Long getMinTxnAmount() {
        return minTxnAmount;
    }

    public Long getMaxEligibleAmount() {
        return maxEligibleAmount;
    }

    public Long getMaxBenefitPerTxn() {
        return maxBenefitPerTxn;
    }

    public Long getMonthlyLimit() {
        return monthlyLimit;
    }

    public Long getDailyLimit() {
        return dailyLimit;
    }

    public Long getQuarterlyLimit() {
        return quarterlyLimit;
    }

    public Long getYearlyLimit() {
        return yearlyLimit;
    }

    public Integer getMonthlyCountLimit() {
        return monthlyCountLimit;
    }

    public Integer getDailyCountLimit() {
        return dailyCountLimit;
    }

    public Integer getQuarterlyCountLimit() {
        return quarterlyCountLimit;
    }

    public Integer getYearlyCountLimit() {
        return yearlyCountLimit;
    }

    public boolean isUseSharedLimit() {
        return useSharedLimit;
    }

    public static final class Builder {
        private long benefitId;
        private BenefitKind benefitKind;
        private CalcMethod calcMethod;
        private BigDecimal benefitValue;
        private Integer stepCount;
        private boolean requirePerformance;
        private String requirePaymentType;
        private Long minTxnAmount;
        private Long maxEligibleAmount;
        private Long maxBenefitPerTxn;
        private Long monthlyLimit;
        private Long dailyLimit;
        private Long quarterlyLimit;
        private Long yearlyLimit;
        private Integer monthlyCountLimit;
        private Integer dailyCountLimit;
        private Integer quarterlyCountLimit;
        private Integer yearlyCountLimit;
        private boolean useSharedLimit;

        private Builder() {
        }

        public Builder benefitId(long benefitId) {
            this.benefitId = benefitId;
            return this;
        }

        public Builder benefitKind(BenefitKind benefitKind) {
            this.benefitKind = benefitKind;
            return this;
        }

        public Builder calcMethod(CalcMethod calcMethod) {
            this.calcMethod = calcMethod;
            return this;
        }

        public Builder benefitValue(BigDecimal benefitValue) {
            this.benefitValue = benefitValue;
            return this;
        }

        public Builder stepCount(Integer stepCount) {
            this.stepCount = stepCount;
            return this;
        }

        public Builder requirePerformance(boolean requirePerformance) {
            this.requirePerformance = requirePerformance;
            return this;
        }

        public Builder requirePaymentType(String requirePaymentType) {
            this.requirePaymentType = requirePaymentType;
            return this;
        }

        public Builder minTxnAmount(Long minTxnAmount) {
            this.minTxnAmount = minTxnAmount;
            return this;
        }

        public Builder maxEligibleAmount(Long maxEligibleAmount) {
            this.maxEligibleAmount = maxEligibleAmount;
            return this;
        }

        public Builder maxBenefitPerTxn(Long maxBenefitPerTxn) {
            this.maxBenefitPerTxn = maxBenefitPerTxn;
            return this;
        }

        public Builder monthlyLimit(Long monthlyLimit) {
            this.monthlyLimit = monthlyLimit;
            return this;
        }

        public Builder dailyLimit(Long dailyLimit) {
            this.dailyLimit = dailyLimit;
            return this;
        }

        public Builder quarterlyLimit(Long quarterlyLimit) {
            this.quarterlyLimit = quarterlyLimit;
            return this;
        }

        public Builder yearlyLimit(Long yearlyLimit) {
            this.yearlyLimit = yearlyLimit;
            return this;
        }

        public Builder monthlyCountLimit(Integer monthlyCountLimit) {
            this.monthlyCountLimit = monthlyCountLimit;
            return this;
        }

        public Builder dailyCountLimit(Integer dailyCountLimit) {
            this.dailyCountLimit = dailyCountLimit;
            return this;
        }

        public Builder quarterlyCountLimit(Integer quarterlyCountLimit) {
            this.quarterlyCountLimit = quarterlyCountLimit;
            return this;
        }

        public Builder yearlyCountLimit(Integer yearlyCountLimit) {
            this.yearlyCountLimit = yearlyCountLimit;
            return this;
        }

        public Builder useSharedLimit(boolean useSharedLimit) {
            this.useSharedLimit = useSharedLimit;
            return this;
        }

        public BenefitRule build() {
            return new BenefitRule(this);
        }
    }
}
