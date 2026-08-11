package com.wallet.engine.model;

import java.util.Collections;
import java.util.List;

/**
 * 카드가 가진 혜택 하나 — 매칭 정보와 계산 규칙을 함께 들고 있다.
 *
 * 카드 단위 계산은 두 단계로 나뉜다:
 * 1) 매칭 — 이 혜택이 이번 결제에 해당하는가 (대상·제외). 여기 담긴 매칭 정보로 판정
 * 2) 계산 — 해당한다면 얼마인가. {@link #getRule()}을 꺼내 BenefitCalculator에 넘김
 *
 * 계산기가 매칭 정보를 보지 못하게 두 관심사를 분리해 둔 것이다.
 */
public final class BenefitCandidate {

    private final TargetType targetType;
    private final Long targetCategoryId;
    private final Long targetMerchantId;
    /** 금액 묶음 코드. 같은 카드 내 같은 코드끼리 기간별 금액 한도를 공유한다. null이면 단독 */
    private final String limitGroupCode;
    /**
     * 횟수 묶음 코드. null이면 limitGroupCode를 따른다.
     *
     * 금액 묶음과 횟수 묶음의 범위가 다를 때 쓴다 — "택시·커피·영화관 합쳐 월 5천원"(금액)에
     * "영화관 3사 합쳐 연 4회"(횟수)가 겹치는 경우, 하나로 묶으면 연 4회가 12회가 된다.
     */
    private final String countGroupCode;
    /** 선택형 혜택 묶음 코드. 같은 코드 중 그달에 고른 선택지만 적용된다. null이면 상시 적용 */
    private final String optionGroupCode;
    /** 이 혜택이 속한 선택지. 선택지 하나가 혜택 여러 개로 이뤄질 수 있어 혜택 id가 아니라 이 값으로 고른다 */
    private final String optionKey;
    /** 이 혜택의 실적 조건이 어느 기간 축인가. 같은 카드에서 혜택마다 다를 수 있다 */
    private final PerformancePeriod performancePeriod;
    private final List<BenefitExclusion> exclusions;
    private final BenefitRule rule;

    private BenefitCandidate(Builder builder) {
        if (builder.targetType == null) {
            throw new IllegalArgumentException("targetType은 필수다");
        }
        if (builder.rule == null) {
            throw new IllegalArgumentException("rule은 필수다");
        }
        validateTarget(builder);
        this.targetType = builder.targetType;
        this.targetCategoryId = builder.targetCategoryId;
        this.targetMerchantId = builder.targetMerchantId;
        this.limitGroupCode = builder.limitGroupCode;
        this.countGroupCode = builder.countGroupCode;
        this.optionGroupCode = builder.optionGroupCode;
        this.optionKey = builder.optionKey;
        this.performancePeriod = builder.performancePeriod == null
                ? PerformancePeriod.MONTH
                : builder.performancePeriod;
        this.exclusions = builder.exclusions == null
                ? List.of()
                : Collections.unmodifiableList(List.copyOf(builder.exclusions));
        this.rule = builder.rule;
    }

    /** 스키마의 ck_benefit_target 제약과 같은 규칙 — 대상 컬럼은 유형에 맞는 것 하나만 채운다 */
    private static void validateTarget(Builder builder) {
        switch (builder.targetType) {
            case CATEGORY -> {
                if (builder.targetCategoryId == null || builder.targetMerchantId != null) {
                    throw new IllegalArgumentException("CATEGORY 대상은 targetCategoryId만 가져야 한다");
                }
            }
            case MERCHANT -> {
                if (builder.targetMerchantId == null || builder.targetCategoryId != null) {
                    throw new IllegalArgumentException("MERCHANT 대상은 targetMerchantId만 가져야 한다");
                }
            }
            case ALL -> {
                if (builder.targetCategoryId != null || builder.targetMerchantId != null) {
                    throw new IllegalArgumentException("ALL 대상은 대상 id를 가질 수 없다");
                }
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getBenefitId() {
        return rule.getBenefitId();
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public Long getTargetCategoryId() {
        return targetCategoryId;
    }

    public Long getTargetMerchantId() {
        return targetMerchantId;
    }

    public String getLimitGroupCode() {
        return limitGroupCode;
    }

    /** 횟수 묶음 코드. 지정이 없으면 금액 묶음을 따른다 */
    public String getCountGroupCode() {
        return countGroupCode == null ? limitGroupCode : countGroupCode;
    }

    public String getOptionGroupCode() {
        return optionGroupCode;
    }

    public String getOptionKey() {
        return optionKey;
    }

    public PerformancePeriod getPerformancePeriod() {
        return performancePeriod;
    }

    public List<BenefitExclusion> getExclusions() {
        return exclusions;
    }

    public BenefitRule getRule() {
        return rule;
    }

    public static final class Builder {
        private TargetType targetType;
        private Long targetCategoryId;
        private Long targetMerchantId;
        private String limitGroupCode;
        private String countGroupCode;
        private String optionGroupCode;
        private String optionKey;
        private PerformancePeriod performancePeriod;
        private List<BenefitExclusion> exclusions;
        private BenefitRule rule;

        private Builder() {
        }

        public Builder targetType(TargetType targetType) {
            this.targetType = targetType;
            return this;
        }

        public Builder targetCategoryId(Long targetCategoryId) {
            this.targetCategoryId = targetCategoryId;
            return this;
        }

        public Builder targetMerchantId(Long targetMerchantId) {
            this.targetMerchantId = targetMerchantId;
            return this;
        }

        public Builder limitGroupCode(String limitGroupCode) {
            this.limitGroupCode = limitGroupCode;
            return this;
        }

        public Builder countGroupCode(String countGroupCode) {
            this.countGroupCode = countGroupCode;
            return this;
        }

        public Builder optionGroupCode(String optionGroupCode) {
            this.optionGroupCode = optionGroupCode;
            return this;
        }

        public Builder optionKey(String optionKey) {
            this.optionKey = optionKey;
            return this;
        }

        public Builder performancePeriod(PerformancePeriod performancePeriod) {
            this.performancePeriod = performancePeriod;
            return this;
        }

        public Builder exclusions(List<BenefitExclusion> exclusions) {
            this.exclusions = exclusions;
            return this;
        }

        public Builder rule(BenefitRule rule) {
            this.rule = rule;
            return this;
        }

        public BenefitCandidate build() {
            return new BenefitCandidate(this);
        }
    }
}
