package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitRule;
import com.wallet.engine.model.TierLimitOverride;

/**
 * 구간별 한도·혜택값 상속 해석기 (benefit_tier_limit).
 *
 * 실적 구간이 판정된 뒤, 그 구간의 값으로 혜택 규칙을 갈아끼운다.
 * 여기서만 "구간 값 NULL = 기본값 상속"이라는 규칙을 다루므로,
 * 계산기는 상한 컬럼의 NULL을 "제약 없음" 한 가지 의미로만 해석하면 된다.
 *
 * 구간 값 0은 상속이 아니라 그대로 0이다 — "이 구간에선 한도 0(혜택 없음)"이라는 뜻.
 *
 * 일 한도는 구간별로 갈리지 않는다 — 스키마에 tier_daily_limit이 없다.
 */
public final class TierLimitResolver {

    /**
     * @param baseRule benefit 테이블 원값으로 만든 규칙
     * @param override 판정된 구간의 덮어쓰기 값. 행이 없으면 {@link TierLimitOverride#none()}
     */
    public BenefitRule resolve(BenefitRule baseRule, TierLimitOverride override) {
        if (baseRule == null) {
            throw new IllegalArgumentException("baseRule은 필수다");
        }
        if (override == null || override.isEmpty()) {
            return baseRule;
        }
        BenefitRule.Builder builder = baseRule.toBuilder();
        if (override.monthlyLimit() != null) {
            builder.monthlyLimit(override.monthlyLimit());
        }
        if (override.quarterlyLimit() != null) {
            builder.quarterlyLimit(override.quarterlyLimit());
        }
        if (override.yearlyLimit() != null) {
            builder.yearlyLimit(override.yearlyLimit());
        }
        if (override.benefitValue() != null) {
            builder.benefitValue(override.benefitValue());
        }
        return builder.build();
    }
}
