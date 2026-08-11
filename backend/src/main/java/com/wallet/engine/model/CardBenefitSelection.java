package com.wallet.engine.model;

import java.util.Collections;
import java.util.List;

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
 * @param stampedBenefitIds 이번 결제로 스탬프가 찍힌 COUNT_STEP 혜택들 ({@link #withStamped} 참조)
 */
public record CardBenefitSelection(
        Long benefitId,
        BenefitKind benefitKind,
        long benefitAmount,
        boolean estimate,
        CapType appliedCap,
        List<Long> stampedBenefitIds
) {

    public CardBenefitSelection {
        stampedBenefitIds = stampedBenefitIds == null
                ? List.of()
                : Collections.unmodifiableList(List.copyOf(stampedBenefitIds));
    }

    /** 매칭되거나 적용 가능한 혜택이 하나도 없는 카드 */
    public static CardBenefitSelection none() {
        return new CardBenefitSelection(null, null, 0L, false, CapType.NONE, List.of());
    }

    public boolean hasBenefit() {
        return benefitId != null;
    }

    /**
     * 스탬프 진행 목록을 붙인 사본.
     *
     * 스탬프는 그 결제에서 다른 혜택을 받았는지와 무관하게 찍히므로 선택 결과와 별개로 모은다.
     * <b>정산은 이 목록의 혜택마다 진행 횟수를 1 올린다.</b> 선택된 혜택이 COUNT_STEP이면
     * 이 목록에 이미 들어 있으므로, 선택분으로 다시 올리면 두 번 세어진다.
     */
    public CardBenefitSelection withStamped(List<Long> stampedBenefitIds) {
        return new CardBenefitSelection(
                benefitId, benefitKind, benefitAmount, estimate, appliedCap, stampedBenefitIds);
    }
}
