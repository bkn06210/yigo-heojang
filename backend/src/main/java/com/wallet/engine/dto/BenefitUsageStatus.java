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
 * @param requirePerformance 전월실적 조건이 걸린 혜택인가. 카드의 performanceMet이 false면 이번 달
 *                           적용되지 않는다 — 화면은 이 둘을 함께 보고 "실적 채우면 받을 수 있어요"를 표시한다
 */
public record BenefitUsageStatus(
        long benefitId,
        String benefitName,
        String limitGroupCode,
        long usedAmount,
        Long monthlyLimit,
        Long remainingLimit,
        BigDecimal usageRate,
        boolean requirePerformance
) {
}
