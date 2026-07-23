package com.wallet.engine.model;

/**
 * 혜택 종류 (benefit.benefit_kind).
 * 계산 대상 여부가 종류마다 다르다:
 * DISCOUNT/POINT는 금액 계산, SPECIAL_PRICE는 혜택값(정가−특가) 사용,
 * GIFT는 금액 환산 불가라 0원(순위 비교 제외), RETROACTIVE는 결제시점 확정 불가라 추천 계산에서 제외.
 */
public enum BenefitKind {
    DISCOUNT,
    POINT,
    SPECIAL_PRICE,
    GIFT,
    RETROACTIVE
}
