package com.wallet.engine.model;

/**
 * 계산 컨텍스트 — 거래 1건과, 그 시점의 소진 상태.
 *
 * 계산기는 DB와 시계를 모른다. 아래 값을 채우는 것은 전부 호출자 책임이다:
 * - monthlyUsedAmount: limit_group_code로 묶인 혜택이면 "그룹 합산" 소진액을 넣는다
 *   (user_benefit_usage를 SUM한 값). 혜택 단독 소진액을 넣으면 묶음 한도가 몇 배로 샌다.
 * - dailyUsedAmount/dailyUsedCount: last_applied_date가 오늘일 때만 유효한 값이다.
 *   오늘이 아니면 0을 넣는다 (날짜 리셋 판정은 계산기 밖).
 * - performanceMet: 실적 구간 판정 결과. "판정된 구간의 min_performance_amount > 0"이면 true.
 * - sharedMonthlyLimit: 판정된 구간의 통합할인한도. null = 통합한도 없는 카드, 0 = 혜택 없음.
 *
 * amountEstimated: 결제금액이 구간 대표값(추천 흐름)이면 true, 실제 확정 금액(정산 재계산)이면
 * false. isEstimate 판정에 필요하며 기본값이 없다 — 흐름마다 의미가 달라 명시를 강제한다.
 */
public final class CalcContext {

    private final long paymentAmount;
    /** 포인트 사용분. 현재 팀 합의상 amount가 이미 차감 후 값이라 항상 0이지만, 계산식은 이 자리를 유지한다 */
    private final long usedPointAmount;
    private final String paymentType;
    private final boolean performanceMet;
    private final boolean amountEstimated;
    private final long monthlyUsedAmount;
    private final int monthlyUsedCount;
    private final long dailyUsedAmount;
    private final int dailyUsedCount;
    private final Long sharedMonthlyLimit;
    private final long sharedLimitUsed;

    private CalcContext(Builder builder) {
        if (builder.paymentAmount <= 0) {
            throw new IllegalArgumentException("결제금액은 양수여야 한다: " + builder.paymentAmount);
        }
        if (builder.usedPointAmount < 0 || builder.usedPointAmount >= builder.paymentAmount) {
            throw new IllegalArgumentException(
                    "포인트 사용액은 0 이상, 결제금액 미만이어야 한다: " + builder.usedPointAmount);
        }
        if (builder.amountEstimated == null) {
            throw new IllegalArgumentException("amountEstimated는 명시가 필수다 (추천=true, 정산=false)");
        }
        this.paymentAmount = builder.paymentAmount;
        this.usedPointAmount = builder.usedPointAmount;
        this.paymentType = builder.paymentType;
        this.performanceMet = builder.performanceMet;
        this.amountEstimated = builder.amountEstimated;
        this.monthlyUsedAmount = builder.monthlyUsedAmount;
        this.monthlyUsedCount = builder.monthlyUsedCount;
        this.dailyUsedAmount = builder.dailyUsedAmount;
        this.dailyUsedCount = builder.dailyUsedCount;
        this.sharedMonthlyLimit = builder.sharedMonthlyLimit;
        this.sharedLimitUsed = builder.sharedLimitUsed;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getPaymentAmount() {
        return paymentAmount;
    }

    public long getUsedPointAmount() {
        return usedPointAmount;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public boolean isPerformanceMet() {
        return performanceMet;
    }

    public boolean isAmountEstimated() {
        return amountEstimated;
    }

    public long getMonthlyUsedAmount() {
        return monthlyUsedAmount;
    }

    public int getMonthlyUsedCount() {
        return monthlyUsedCount;
    }

    public long getDailyUsedAmount() {
        return dailyUsedAmount;
    }

    public int getDailyUsedCount() {
        return dailyUsedCount;
    }

    public Long getSharedMonthlyLimit() {
        return sharedMonthlyLimit;
    }

    public long getSharedLimitUsed() {
        return sharedLimitUsed;
    }

    public static final class Builder {
        private long paymentAmount;
        private long usedPointAmount;
        private String paymentType;
        private boolean performanceMet;
        private Boolean amountEstimated;
        private long monthlyUsedAmount;
        private int monthlyUsedCount;
        private long dailyUsedAmount;
        private int dailyUsedCount;
        private Long sharedMonthlyLimit;
        private long sharedLimitUsed;

        private Builder() {
        }

        public Builder paymentAmount(long paymentAmount) {
            this.paymentAmount = paymentAmount;
            return this;
        }

        public Builder usedPointAmount(long usedPointAmount) {
            this.usedPointAmount = usedPointAmount;
            return this;
        }

        public Builder paymentType(String paymentType) {
            this.paymentType = paymentType;
            return this;
        }

        public Builder performanceMet(boolean performanceMet) {
            this.performanceMet = performanceMet;
            return this;
        }

        public Builder amountEstimated(boolean amountEstimated) {
            this.amountEstimated = amountEstimated;
            return this;
        }

        public Builder monthlyUsedAmount(long monthlyUsedAmount) {
            this.monthlyUsedAmount = monthlyUsedAmount;
            return this;
        }

        public Builder monthlyUsedCount(int monthlyUsedCount) {
            this.monthlyUsedCount = monthlyUsedCount;
            return this;
        }

        public Builder dailyUsedAmount(long dailyUsedAmount) {
            this.dailyUsedAmount = dailyUsedAmount;
            return this;
        }

        public Builder dailyUsedCount(int dailyUsedCount) {
            this.dailyUsedCount = dailyUsedCount;
            return this;
        }

        public Builder sharedMonthlyLimit(Long sharedMonthlyLimit) {
            this.sharedMonthlyLimit = sharedMonthlyLimit;
            return this;
        }

        public Builder sharedLimitUsed(long sharedLimitUsed) {
            this.sharedLimitUsed = sharedLimitUsed;
            return this;
        }

        public CalcContext build() {
            return new CalcContext(this);
        }
    }
}
