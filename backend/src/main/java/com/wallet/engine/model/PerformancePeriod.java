package com.wallet.engine.model;

/**
 * 실적 기준 기간 (benefit.performance_period · performance_tier.period_type).
 *
 * 한 카드가 두 기간의 구간표를 함께 가질 수 있다 — 일상 영역은 전월 실적으로, 특정 영역은
 * 전분기 실적으로 판정하는 카드가 있다. 그래서 실적 판정 결과가 카드당 하나가 아니라 기간당 하나다.
 *
 * 기간을 무시하고 한 구간표로 판정하면, 전분기 100만원 조건이 전월 100만원으로 읽혀
 * 실제로는 충족한 회원이 미충족으로 판정된다(에러 없이 혜택만 사라진다).
 */
public enum PerformancePeriod {
    MONTH,
    QUARTER;

    /** DB 원값 → enum. NULL은 기본값 MONTH로 본다(스키마 DEFAULT와 같은 규칙) */
    public static PerformancePeriod from(String value) {
        return value == null ? MONTH : valueOf(value);
    }
}
