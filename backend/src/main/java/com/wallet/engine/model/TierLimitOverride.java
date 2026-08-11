package com.wallet.engine.model;

import java.math.BigDecimal;

/**
 * 실적 구간별 개별한도·혜택값 덮어쓰기 값 (benefit_tier_limit 한 행).
 *
 * 각 값의 null은 "이 구간에 지정 없음 = benefit 기본값 상속"이다.
 * 상한 컬럼에서의 null("제약 없음")과 의미가 다르므로, 이 구분은 TierLimitResolver 안에서만 다룬다.
 * 0은 상속이 아니라 그대로 0이다 — "이 구간에선 한도 0(혜택 없음)".
 *
 * 개별 인자로 늘어놓지 않고 묶은 이유: 기간별 한도가 전부 Long이라 인자 자리를 바꿔 넣어도
 * 컴파일이 통과하고, 잘못된 기간의 한도가 적용되어 금액만 조용히 틀린다.
 */
public record TierLimitOverride(
        Long monthlyLimit,
        Long quarterlyLimit,
        Long yearlyLimit,
        BigDecimal benefitValue) {

    private static final TierLimitOverride NONE = new TierLimitOverride(null, null, null, null);

    /** 판정된 구간에 benefit_tier_limit 행이 없을 때 — 기본값을 전부 유지한다 */
    public static TierLimitOverride none() {
        return NONE;
    }

    public boolean isEmpty() {
        return monthlyLimit == null && quarterlyLimit == null && yearlyLimit == null && benefitValue == null;
    }
}
