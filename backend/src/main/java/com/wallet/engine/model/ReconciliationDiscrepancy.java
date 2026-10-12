package com.wallet.engine.model;

/**
 * 불일치 한 건 — 저장된 누적값이 원본에서 다시 구한 값과 다르다.
 *
 * @param target        무엇을 대조했는지
 * @param kind          어떻게 어긋났는지
 * @param userCardId    보유카드 ID
 * @param benefitId     혜택 ID. 카드 월 상태(실적·통합한도)의 불일치면 null
 * @param baseYearMonth 기준 연월 (YYYY-MM)
 * @param storedValue   상태 테이블에 저장된 값. 상태 행이 없으면 null
 * @param sourceValue   원본에서 다시 구한 값. 뒷받침하는 원본 기록이 없으면 null
 */
public record ReconciliationDiscrepancy(ReconciliationTarget target,
                                        DiscrepancyKind kind,
                                        long userCardId,
                                        Long benefitId,
                                        String baseYearMonth,
                                        Long storedValue,
                                        Long sourceValue) {

    /** 저장값 − 원본값. 양수면 상태가 원본보다 많이 쌓였고(한도가 덜 남아 보인다), 음수면 덜 쌓였다 */
    public long difference() {
        return valueOrZero(storedValue) - valueOrZero(sourceValue);
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
