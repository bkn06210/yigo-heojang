package com.wallet.notification.batch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 실적 부족 후보 계산을 위한 조회 결과 투영 (규칙 문서 2장).
 * <p>
 * 대상 필터(회원 활성, 보유카드 활성, MONTH 구간표 존재, performance_shortage_enabled)는
 * SQL의 JOIN·WHERE에서 전부 처리한다. 그래서 이 행이 조회됐다는 것 자체가
 * "이 카드는 실적 부족 알림 평가 대상이다"라는 뜻이고, 계산기는 이 필터를 다시 걸 필요가 없다.
 * <p>
 * targetPerformance는 MONTH 구간 중 첫 번째 양수 구간의 최소 실적(2.2절)을 SQL의
 * MIN(min_performance_amount)로 미리 계산해서 담아온다.
 * <p>
 * currentPerformanceAmount는 기준월 상태 행이 없으면 SQL의 COALESCE(...,0)으로
 * 이미 0이 채워져서 온다 — "행이 없으면 0"이라는 2.3절 규칙을 SQL에서 처리했다.
 */
@Getter
@Builder
@AllArgsConstructor
public class PerformanceShortageCandidateRow {
    private long userCardId;
    private long memberId;
    private long cardId;
    private String cardName;
    private long targetPerformance;
    private long currentPerformanceAmount;
}