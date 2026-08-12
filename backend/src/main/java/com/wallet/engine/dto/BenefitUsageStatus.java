package com.wallet.engine.dto;

import java.math.BigDecimal;

/**
 * 혜택 한 개의 이번 달 이용 현황 — 현황 조회(CardMonthlyStatus)의 benefits[] 항목.
 *
 * 묶음 한도(limitGroupCode) 소속이면 usedAmount·monthlyLimit·remainingLimit·usageRate가
 * 모두 <b>그룹 기준</b> 값으로 내려간다(같은 코드끼리 같은 값). 화면은 같은 코드를 한 줄로 묶어 표시한다.
 *
 * <b>remainingLimit은 이 혜택의 개별 잔액일 뿐이다.</b> 통합할인한도를 쓰는 혜택은 실제로 받을 때
 * 카드의 통합 잔여(sharedLimit − sharedLimitUsed)에 한 번 더 막힌다. 두 잔액은 성격이 달라
 * 합치지 않는다 — 화면은 통합 잔여를 카드 단위로 한 줄 두고, 개별 잔여를 혜택마다 표시한다.
 *
 * @param benefitId          혜택 ID
 * @param benefitName        혜택명
 * @param limitGroupCode     묶음 한도 코드. null이면 이 혜택 단독
 * @param usedAmount         이번 달 누적 혜택액. 묶음 소속이면 그룹 전체 누적액
 * @param monthlyLimit       월 한도(없으면 null). 구간별 한도가 있으면 판정된 구간의 한도. 묶음 소속이면 그룹 공유 한도
 * @param remainingLimit     잔여 한도(monthlyLimit − usedAmount, 한도 없으면 null)
 * @param usageRate          이용률(%). 한도 없으면 null
 * @param requirePerformance 실적 조건이 걸린 혜택인가
 * @param performanceMet     <b>이 혜택의 실적 축</b>으로 판정한 충족 여부.
 *                           카드의 performanceMet은 전월 축 하나뿐이라, 전분기 실적을 쓰는 혜택은
 *                           값이 다를 수 있다(월 40만원은 채웠지만 분기 100만원은 못 채운 상태).
 *                           "지금 받을 수 있나"는 카드가 아니라 <b>이 값</b>과 requirePerformance를
 *                           함께 봐야 한다
 * @param useSharedLimit     통합할인한도를 쓰는 혜택인가. true면 remainingLimit이 남아 있어도
 *                           카드의 통합 잔여(sharedLimit − sharedLimitUsed)가 0이면 실제로는 못 받는다
 */
public record BenefitUsageStatus(
        long benefitId,
        String benefitName,
        String limitGroupCode,
        long usedAmount,
        Long monthlyLimit,
        Long remainingLimit,
        BigDecimal usageRate,
        boolean requirePerformance,
        boolean performanceMet,
        boolean useSharedLimit
) {
}
