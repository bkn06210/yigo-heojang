package com.wallet.engine.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 카드 한 장의 이번 달 상태 — 실적 판정 결과와 한도 소진 현황.
 *
 * <b>실적 판정 결과가 기간마다 따로다.</b> 한 카드가 전월 축과 전분기 축 구간표를 함께 갖는
 * 경우가 있고, 혜택은 자기 performance_period가 가리키는 축으로 판정돼야 한다.
 * 하나로 합치면 전분기 100만원 조건이 전월 100만원으로 읽혀, 충족한 회원이 미충족으로 판정된다.
 *
 * 통합할인한도는 월 단위 개념이라 전월 축 판정 결과의 값을 쓴다.
 *
 * @param monthStatus        전월 실적으로 판정한 구간
 * @param quarterStatus      전분기 실적으로 판정한 구간. 분기 구간표가 없는 카드면 null
 * @param sharedLimitUsed    통합한도 소진액
 * @param usages             혜택별 소진 현황. 목록에 없는 혜택은 소진 0으로 본다
 * @param selectedOptionKeys 선택형 혜택 묶음별로 그달에 고른 선택지 (option_group_code → option_key).
 *                           묶음이 여기 없으면 그달에 고르지 않은 것이라 그 묶음의 혜택은 하나도 적용되지 않는다
 */
public record CardState(
        PerformanceStatus monthStatus,
        PerformanceStatus quarterStatus,
        long sharedLimitUsed,
        List<BenefitUsage> usages,
        Map<String, String> selectedOptionKeys
) {

    public CardState {
        if (monthStatus == null) {
            throw new IllegalArgumentException("전월 실적 판정 결과는 필수다");
        }
        usages = usages == null ? List.of() : Collections.unmodifiableList(List.copyOf(usages));
        selectedOptionKeys = selectedOptionKeys == null ? Map.of() : Map.copyOf(selectedOptionKeys);
    }

    /** 분기 구간표도 선택형 혜택도 없는 카드 */
    public CardState(PerformanceStatus monthStatus, long sharedLimitUsed, List<BenefitUsage> usages) {
        this(monthStatus, null, sharedLimitUsed, usages, Map.of());
    }

    /**
     * 이 기간 축의 실적 충족 여부.
     *
     * 분기 구간표가 없는 카드에 분기 혜택이 달려 있으면 미충족으로 본다 — 판정할 구간표가 없으니
     * 조건을 충족했다고 볼 근거가 없다. 혜택을 주는 쪽으로 뭉개면 실적 없이 지급된다.
     */
    public boolean performanceMet(PerformancePeriod period) {
        PerformanceStatus status = statusOf(period);
        return status != null && status.performanceMet();
    }

    public PerformanceStatus statusOf(PerformancePeriod period) {
        return period == PerformancePeriod.QUARTER ? quarterStatus : monthStatus;
    }

    /** 통합할인한도. null = 통합한도 없는 카드, 0 = 혜택 없음 */
    public Long sharedMonthlyLimit() {
        return monthStatus.sharedMonthlyLimit();
    }

    public BenefitUsage usageOf(long benefitId) {
        return usages.stream()
                .filter(usage -> usage.benefitId() == benefitId)
                .findFirst()
                .orElseGet(() -> BenefitUsage.empty(benefitId));
    }

    /** 그달에 고른 선택지. 안 골랐으면 null */
    public String selectedOptionKeyOf(String optionGroupCode) {
        return selectedOptionKeys.get(optionGroupCode);
    }
}
