package com.wallet.engine.model;

/**
 * 대조 불가 한 건 — 저장값은 있지만 원본에 그 값을 다시 구할 흔적이 없다.
 *
 * 불일치가 아니다. 맞는지 틀린지를 원본으로는 가릴 수 없다는 뜻이며,
 * "이 값은 어긋나도 찾아낼 수 없다"는 것 자체가 알아 둬야 할 사실이라 목록으로 남긴다.
 *
 * @param target        무엇을 대조하려 했는지
 * @param userCardId    보유카드 ID
 * @param benefitId     혜택 ID
 * @param baseYearMonth 기준 연월 (YYYY-MM)
 * @param storedValue   상태 테이블에 저장된 값
 * @param tracedValue   원본에 흔적이 남은 만큼. 저장값의 일부만 설명한다
 */
public record ReconciliationUnverifiable(ReconciliationTarget target,
                                         long userCardId,
                                         long benefitId,
                                         String baseYearMonth,
                                         long storedValue,
                                         long tracedValue) {
}
