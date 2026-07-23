package com.wallet.engine.model;

/**
 * 카드 한 장의 계산 결과 — 이 결제에 적용될 혜택 1개와 그 금액.
 *
 * 한 결제에 여러 혜택이 매칭돼도 합산하지 않고 혜택액이 가장 큰 1개만 적용한다
 * (실제 약관의 "타 할인과 중복 불가"가 기본이고, expense.applied_benefit_id도 단수다).
 *
 * @param benefitId   적용될 혜택 id. 적용 가능한 혜택이 하나도 없으면 null
 * @param benefitKind 적용될 혜택의 종류. 없으면 null
 * @param benefitAmount 혜택액(원). 한도 소진으로 0원일 수도 있다
 * @param estimate    예상 여부
 * @param appliedCap  혜택액을 깎은 상한
 */
public record CardBenefitSelection(
        Long benefitId,
        BenefitKind benefitKind,
        long benefitAmount,
        boolean estimate,
        CapType appliedCap
) {

    /** 매칭되거나 적용 가능한 혜택이 하나도 없는 카드 */
    public static CardBenefitSelection none() {
        return new CardBenefitSelection(null, null, 0L, false, CapType.NONE);
    }

    public boolean hasBenefit() {
        return benefitId != null;
    }
}
