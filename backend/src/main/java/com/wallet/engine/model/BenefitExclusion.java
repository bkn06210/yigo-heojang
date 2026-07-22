package com.wallet.engine.model;

/**
 * 혜택 적용 예외 한 건 (benefit_exclusion 한 행).
 *
 * value는 id가 아니라 코드 문자열이다 — CATEGORY면 category_code, MERCHANT면 merchant_code.
 * (스키마에서 값에 FK를 못 거는 대신 시드 가독성을 택한 결과)
 *
 * @param type  제외 유형
 * @param value 제외 값. 유형에 따라 해석이 달라진다
 */
public record BenefitExclusion(ExclusionType type, String value) {

    public BenefitExclusion {
        if (type == null) {
            throw new IllegalArgumentException("제외 유형은 필수다");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("제외 값은 필수다");
        }
    }
}
