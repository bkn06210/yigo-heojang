package com.wallet.engine.model;

import java.math.BigDecimal;

/**
 * 실적 진행률 — 당월 누적 실적이 다음 구간에 얼마나 다가갔는지.
 *
 * 실적 충족 판정(PerformanceStatus)은 전월실적 기준인데, 이 진행률은 당월 누적 기준이다.
 * 축이 다르므로 "충족했지만 이번 달 진행률은 낮음" 같은 상태가 정상적으로 나올 수 있다.
 *
 * @param targetPerformance    아직 도달하지 못한 가장 낮은 구간의 최소실적금액.
 *                             전 구간을 이미 넘었으면 최고 구간 금액
 * @param remainingPerformance 목표까지 남은 금액. 초과 달성 시 음수가 아니라 0
 * @param achievementRate      달성률(%). 실적 조건 없는 카드는 0.0이 아니라 null
 */
public record PerformanceProgress(long targetPerformance, long remainingPerformance, BigDecimal achievementRate) {
}
