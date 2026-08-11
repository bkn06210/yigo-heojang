package com.wallet.engine.dto;

import javax.validation.constraints.NotNull;

/**
 * 결제 취소 정산 요청 — 취소된 소비내역 하나를 가리킨다.
 *
 * 취소는 앱 기능이 아니라 카드사/마이데이터로 유입되는 사실이라, 요청은 대상 소비내역 id 하나뿐이다.
 * 회원 id는 요청 body가 아니라 토큰에서 꺼낸다(남의 소비내역 취소 방지).
 */
public class SettlementCancelRequest {

    @NotNull(message = "expenseId는 필수입니다.")
    private Long expenseId;

    public Long getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(Long expenseId) {
        this.expenseId = expenseId;
    }
}
