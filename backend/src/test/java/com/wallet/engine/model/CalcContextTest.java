package com.wallet.engine.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CalcContextTest {

    @Test
    void 결제금액_0_이하는_예외다() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> CalcContext.builder().paymentAmount(0L).amountEstimated(true).build());
        assertThatIllegalArgumentException().isThrownBy(
                () -> CalcContext.builder().paymentAmount(-1000L).amountEstimated(true).build());
    }

    @Test
    void amountEstimated_미지정은_예외다() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> CalcContext.builder().paymentAmount(10000L).build());
    }

    @Test
    void 포인트_사용액은_결제금액_미만이어야_한다() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> CalcContext.builder().paymentAmount(10000L).usedPointAmount(10000L).amountEstimated(true).build());
        assertThatIllegalArgumentException().isThrownBy(
                () -> CalcContext.builder().paymentAmount(10000L).usedPointAmount(-1L).amountEstimated(true).build());
    }

    @Test
    void 소진_상태_미지정은_0이_기본값이다() {
        CalcContext context = CalcContext.builder()
                .paymentAmount(10000L)
                .amountEstimated(true)
                .build();

        assertThat(context.getUsedPointAmount()).isZero();
        assertThat(context.getMonthlyUsedAmount()).isZero();
        assertThat(context.getMonthlyUsedCount()).isZero();
        assertThat(context.getDailyUsedAmount()).isZero();
        assertThat(context.getDailyUsedCount()).isZero();
        assertThat(context.getSharedMonthlyLimit()).isNull();
        assertThat(context.getSharedLimitUsed()).isZero();
        assertThat(context.getPaymentType()).isNull();
    }
}
