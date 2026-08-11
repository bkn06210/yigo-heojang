package com.wallet.engine.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 카드 한 장의 월 실적·혜택 이용 현황 — 개별 카드 상세 조회(#3)와 취소 정산 응답이 공유하는 형식.
 *
 * 저장값(전월실적·당월누적·소진액)에 파생값(목표·달성률·잔여·이용률)을 조회 시 계산해 얹는다.
 * 파생값을 저장하지 않는 원칙에 따라 이 응답은 매번 계산으로 조립된다.
 *
 * @param prevPerformanceAmount    전월 실적(현재 구간·통합한도 판정 기준)
 * @param targetPerformance        실적 목표 — 당월누적으로 아직 못 넘은 가장 낮은 구간의 최소실적금액
 * @param remainingPerformance     남은 실적(target − current). 초과 달성 시 0으로 클램프
 * @param achievementRate          달성률(%). 실적 조건 없는 카드(target=0)는 null
 * @param performanceMet           전월실적으로 판정된 구간의 min_performance_amount > 0
 * @param sharedLimit              현재 구간의 월 통합할인한도. null=통합한도 없는 카드, 0=혜택 없음
 * @param benefits                 혜택별 이용 현황. 묶음 한도는 그룹 기준 값으로 내려간다
 */
public record CardMonthlyStatus(
        long userCardId,
        String cardName,
        String yearMonth,
        long prevPerformanceAmount,
        long targetPerformance,
        long currentPerformanceAmount,
        long remainingPerformance,
        BigDecimal achievementRate,
        boolean performanceMet,
        Long sharedLimit,
        long sharedLimitUsed,
        List<BenefitUsageStatus> benefits
) {
}
