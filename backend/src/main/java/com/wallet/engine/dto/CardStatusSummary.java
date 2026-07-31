package com.wallet.engine.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 전체 보유 카드 현황(#2)의 카드 한 장 요약.
 *
 * 실적 관련 값은 개별 상세(#3, {@link CardMonthlyStatus})와 <b>같은 계산으로 조립</b>한다 —
 * 목록에서 본 달성률과 상세에서 본 달성률이 다르면 안 되므로, 상세를 먼저 만들고 여기로 접는다.
 * 상세와 다른 점은 두 가지뿐이다: 전월실적(prevPerformanceAmount)을 빼고,
 * 혜택은 이용 현황 전체가 아니라 "아직 쓸 수 있는 것"만 요약({@link BenefitSummary})한다.
 *
 * @param achievementRate 달성률(%). 실적 조건 없는 카드(목표 0)는 null
 * @param sharedLimit     현재 구간의 월 통합할인한도. null=통합한도 없는 카드, 0=혜택 없음
 * @param benefitsSummary 잔여가 남은 혜택 요약. 묶음 한도는 그룹당 한 줄로 접힌다
 */
public record CardStatusSummary(
        long userCardId,
        String cardName,
        String yearMonth,
        long currentPerformanceAmount,
        long targetPerformance,
        long remainingPerformance,
        BigDecimal achievementRate,
        boolean performanceMet,
        Long sharedLimit,
        long sharedLimitUsed,
        List<BenefitSummary> benefitsSummary
) {
}
