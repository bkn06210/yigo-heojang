package com.wallet.engine.model;

import java.util.Collections;
import java.util.List;

/**
 * 카드 한 장으로 소비 목록을 결제했을 때의 결과 — 총 혜택액과 혜택별 내역.
 *
 * 혜택별 내역을 함께 내리는 것은 "왜 이 카드가 좋은가"를 답에 쓰기 위해서다.
 * 총액만 있으면 "월 12,000원"까지만 말할 수 있고 "커피 적립이 대부분"은 말할 수 없다.
 *
 * @param totalBenefitAmount 총 혜택액(원)
 * @param breakdowns         혜택별 내역. 혜택액이 큰 순서, 동점이면 benefit_id 오름차순
 */
public record SpendingSimulation(long totalBenefitAmount, List<BenefitBreakdown> breakdowns) {

    public SpendingSimulation {
        breakdowns = breakdowns == null ? List.of() : Collections.unmodifiableList(List.copyOf(breakdowns));
    }

    /** 소비가 없거나 받을 혜택이 하나도 없는 카드 */
    public static SpendingSimulation empty() {
        return new SpendingSimulation(0L, List.of());
    }

    /**
     * 혜택 하나의 시뮬레이션 결과.
     *
     * appliedCount는 혜택을 받은 횟수다. COUNT_STEP(N회마다 지급)에서는 스탬프가 찍힌 횟수이며
     * 실제 지급 횟수가 아니다 — 소진 누적 규칙이 정산과 같아야 해서 그쪽 정의를 그대로 따른다.
     */
    public record BenefitBreakdown(long benefitId, long benefitAmount, int appliedCount) {
    }
}
