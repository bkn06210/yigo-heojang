package com.wallet.engine.dto;

/**
 * 결제 가산 정산의 결과 — 소비내역에 기록할 적용 혜택과 혜택액.
 *
 * 엔진은 혜택을 계산해 이 둘을 돌려주고, 소비내역 도메인이 이 값을 expense 행에 저장한다
 * (엔진은 expense를 쓰지 않는다 — CLAUDE.md 원칙). 상태 가산은 엔진이 이미 끝낸 상태다.
 *
 * @param appliedBenefitId 적용된 혜택 ID. 적용 가능한 혜택이 없으면 null
 * @param discountAmount   실제 받은 할인/적립액. 한도 소진으로 0원일 수 있다
 */
public record PaymentSettlementResult(
        Long appliedBenefitId,
        long discountAmount
) {
}
