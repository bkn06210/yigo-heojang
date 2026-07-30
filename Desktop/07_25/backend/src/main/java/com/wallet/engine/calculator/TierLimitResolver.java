package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitRule;

import java.math.BigDecimal;

/**
 * 구간별 한도·혜택값 상속 해석기 (benefit_tier_limit).
 *
 * 실적 구간이 판정된 뒤, 그 구간의 값으로 혜택 규칙을 갈아끼운다.
 * 여기서만 "구간 값 NULL = 기본값 상속"이라는 규칙을 다루므로,
 * 계산기는 상한 컬럼의 NULL을 "제약 없음" 한 가지 의미로만 해석하면 된다.
 *
 * 구간 값 0은 상속이 아니라 그대로 0이다 — "이 구간에선 한도 0(혜택 없음)"이라는 뜻.
 */
public final class TierLimitResolver {

    /**
     * @param baseRule         benefit 테이블 원값으로 만든 규칙
     * @param tierMonthlyLimit 구간별 월 한도. null이면 baseRule의 monthlyLimit 유지
     * @param tierBenefitValue 구간별 혜택값(율 또는 정액). null이면 baseRule의 benefitValue 유지
     */
    public BenefitRule resolve(BenefitRule baseRule, Long tierMonthlyLimit, BigDecimal tierBenefitValue) {
        if (baseRule == null) {
            throw new IllegalArgumentException("baseRule은 필수다");
        }
        if (tierMonthlyLimit == null && tierBenefitValue == null) {
            return baseRule;
        }
        BenefitRule.Builder builder = baseRule.toBuilder();
        if (tierMonthlyLimit != null) {
            builder.monthlyLimit(tierMonthlyLimit);
        }
        if (tierBenefitValue != null) {
            builder.benefitValue(tierBenefitValue);
        }
        return builder.build();
    }
}
