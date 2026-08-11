package com.wallet.engine.model;

/**
 * 혜택액을 깎은 상한의 종류 (min-clamp 계열).
 * 게이트 실패(NotApplicableReason)와 축이 다르다 — 게이트는 "적용 자체가 안 됨",
 * 상한은 "적용됐으나 금액이 깎임"(전액 소진이면 0원).
 *
 * 여러 상한이 동시에 걸리면(동률) 파이프라인 순서
 * (결제금액 → 대상금액 → 건당 → 일 → 월 → 분기 → 연 → 통합)의 첫 번째를 기록한다 — 테스트 재현성.
 * isEstimate 판정(RATE + 상한 도달 → 확정)과 추천 근거 문구 생성의 재료.
 *
 * 기간 상한은 일·월·분기·연 네 축이 각각 따로다. 분기 한도를 월 한도로 환산해 넣으면
 * 매달 리셋되어 실제의 세 배가 나가므로, 축마다 별도 값으로 다룬다.
 */
public enum CapType {
    NONE,
    PAYMENT_AMOUNT,
    MAX_ELIGIBLE_AMOUNT,
    MAX_BENEFIT_PER_TXN,
    DAILY_LIMIT,
    MONTHLY_LIMIT,
    QUARTERLY_LIMIT,
    YEARLY_LIMIT,
    SHARED_LIMIT
}
