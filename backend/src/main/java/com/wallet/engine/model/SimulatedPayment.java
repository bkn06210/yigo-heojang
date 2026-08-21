package com.wallet.engine.model;

import java.time.LocalDate;

/**
 * 시뮬레이션에 흘려보낼 결제 한 건 — 결제 내용과 날짜.
 *
 * 날짜가 따로 있는 것은 일 한도 때문이다. {@link PaymentRequest}는 "어디서 얼마를"만 담고
 * 언제인지는 모르는데, 일 한도·일 횟수는 날짜가 바뀌면 리셋된다. 날짜 없이 흘려보내면
 * 하루치 한도로 한 달을 계산하게 된다.
 *
 * @param paymentDate 결제일. 이 값이 바뀌는 순간 일 소진이 리셋된다
 * @param request     결제 내용 (가맹점·금액·결제수단)
 */
public record SimulatedPayment(LocalDate paymentDate, PaymentRequest request) {

    public SimulatedPayment {
        if (paymentDate == null) {
            throw new IllegalArgumentException("결제일은 필수다 — 일 한도 판정에 쓴다");
        }
        if (request == null) {
            throw new IllegalArgumentException("결제 내용은 필수다");
        }
    }
}
