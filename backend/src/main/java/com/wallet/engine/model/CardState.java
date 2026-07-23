package com.wallet.engine.model;

import java.util.Collections;
import java.util.List;

/**
 * 카드 한 장의 이번 달 상태 — 실적 판정 결과와 한도 소진 현황.
 *
 * @param performanceMet     실적 충족 여부. 판정된 구간의 min_performance_amount > 0이면 true
 * @param sharedMonthlyLimit 판정된 구간의 통합할인한도. null = 통합한도 없는 카드, 0 = 혜택 없음
 * @param sharedLimitUsed    통합한도 소진액
 * @param usages             혜택별 소진 현황. 목록에 없는 혜택은 소진 0으로 본다
 */
public record CardState(
        boolean performanceMet,
        Long sharedMonthlyLimit,
        long sharedLimitUsed,
        List<BenefitUsage> usages
) {

    public CardState {
        usages = usages == null ? List.of() : Collections.unmodifiableList(List.copyOf(usages));
    }

    public BenefitUsage usageOf(long benefitId) {
        return usages.stream()
                .filter(usage -> usage.benefitId() == benefitId)
                .findFirst()
                .orElseGet(() -> BenefitUsage.empty(benefitId));
    }
}
