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
    /** 묶음 한도 코드. 같은 카드 내 같은 코드끼리 monthly_limit을 공유한다. null이면 단독 */
    private final String limitGroupCode;
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
