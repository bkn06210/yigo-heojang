package com.wallet.engine.model;

/**
 * 혜택 종류 (benefit.benefit_kind).
 * 계산 대상 여부가 종류마다 다르다:
 * DISCOUNT/POINT는 금액 계산, SPECIAL_PRICE는 혜택값(정가−특가) 사용.
 * GIFT는 결제 트랜잭션이 없어(카드 제시뿐) 매칭·계산 대상에서 제외하고 카드 상세 화면이 별도로 표시하며,
 * RETROACTIVE는 결제시점 확정 불가라 마찬가지로 제외한다.
 * INSTALLMENT_FREE는 할부수수료를 면제하는 혜택이라 결제금액 기준 할인액으로 환산할 수 없어 함께 제외한다.
 */
public enum BenefitKind {
    DISCOUNT,
    POINT,
    SPECIAL_PRICE,
    GIFT,
    INSTALLMENT_FREE,
    RETROACTIVE
}
