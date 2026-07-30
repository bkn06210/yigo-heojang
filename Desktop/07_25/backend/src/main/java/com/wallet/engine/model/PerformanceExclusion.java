package com.wallet.engine.model;

/**
 * 전월실적 제외 규칙 한 건 (performance_exclusion 한 행).
 *
 * value는 유형에 따라 해석이 다르다 — CATEGORY면 category_code, PAYMENT_TYPE이면
 * payment_type 문자열, MIN_TXN_AMOUNT면 숫자 문자열이다. 값에 FK가 없어(코드 문자열)
 * 오타·판정 불가값이 섞여 들어올 수 있으므로 계산기가 해석하며 걸러낸다.
 *
 * @param type  제외 유형
 * @param value 제외 값. 유형에 따라 해석이 달라진다
 */
public record PerformanceExclusion(PerformanceExclusionType type, String value) {

    public PerformanceExclusion {
        if (type == null) {
            throw new IllegalArgumentException("제외 유형은 필수다");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("제외 값은 필수다");
        }
    }
}
