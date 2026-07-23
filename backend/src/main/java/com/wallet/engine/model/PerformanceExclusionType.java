package com.wallet.engine.model;

/**
 * 전월실적 제외 규칙의 종류 (performance_exclusion.exclusion_type).
 *
 * 혜택 제외(ExclusionType)와 값 집합이 다르다 — 이쪽엔 MERCHANT가 없고 MIN_TXN_AMOUNT가 있다.
 * (실적은 가맹점 단위로 제외하지 않고, 건당 최소금액 미만을 제외한다)
 * 두 테이블이 나뉜 이유가 여기 있으므로 enum을 합치지 않는다 — 합치면 어느 쪽이든
 * 도달할 수 없는 switch 분기가 생긴다.
 *
 *   CATEGORY          category_code. 대분류면 하위 중분류 결제까지 제외(상향 매칭)
 *   PAYMENT_TYPE      expense.payment_type (예: SIMPLE_PAY)
 *   TRANSACTION_ATTR  거래 속성 (INTEREST_FREE / DISCOUNTED / OVERSEAS)
 *   MIN_TXN_AMOUNT    숫자 문자열. 그 금액 미만 거래 제외
 */
public enum PerformanceExclusionType {
    CATEGORY,
    PAYMENT_TYPE,
    TRANSACTION_ATTR,
    MIN_TXN_AMOUNT;

    /**
     * DB 문자열 → 유형. 모르는 값은 스키마 위반(버그)이므로 예외다.
     * performance_exclusion에는 CHECK 제약이 없어 오타난 타입이 DB에서 안 걸린다 —
     * 여기서 fail-fast 해야 조용히 규칙이 통째로 무시되는 일이 없다.
     */
    public static PerformanceExclusionType from(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            throw new IllegalArgumentException("제외 유형은 필수다");
        }
        try {
            return valueOf(rawType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("알 수 없는 실적 제외 유형: " + rawType);
        }
    }
}
