package com.wallet.engine.model;

/**
 * 혜택 미적용 사유 — 게이트(적용 조건) 실패 전용.
 * 한도 소진은 여기 넣지 않는다: 조건은 전부 충족하고 계산 결과가 0원인 것이므로
 * "적용 + 0원 + CapType"으로 표현한다 (게이트와 클램프의 축 분리).
 *
 * 선언 순서 = 게이트 검사 순서 = 사유 우선순위 (여러 게이트가 동시에 실패해도 첫 번째만 기록).
 */
public enum NotApplicableReason {
    RETROACTIVE_EXCLUDED,
    /** 증정은 결제 이벤트가 없어 매칭·계산 대상이 아니다. 카드 상세 화면이 별도 조회로 표시한다 */
    GIFT_EXCLUDED,
    /** 무이자할부는 할부수수료 면제라 결제금액 기준 할인액으로 환산할 수 없다. 카드 상세 화면이 정보로만 표시한다 */
    INSTALLMENT_FREE_EXCLUDED,
    PERFORMANCE_NOT_MET,
    PAYMENT_TYPE_MISMATCH,
    MIN_TXN_AMOUNT_NOT_MET,
    DAILY_COUNT_EXCEEDED,
    MONTHLY_COUNT_EXCEEDED,
    QUARTERLY_COUNT_EXCEEDED,
    YEARLY_COUNT_EXCEEDED
}
