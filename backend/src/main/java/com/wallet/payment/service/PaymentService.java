package com.wallet.payment.service;

import com.wallet.payment.dto.PaymentProcessResponse;
import com.wallet.payment.dto.PaymentRequest;
import com.wallet.payment.dto.PaymentResultResponse;

public interface PaymentService {

    /**
     * @param idempotencyKey 클라이언트가 결제 시도마다 만든 키. 같은 키로 다시 오면 결제하지 않고 처음 결과를 돌려준다.
     *                       null이면 멱등 처리를 하지 않는다
     */
    PaymentProcessResponse processPayment(
            Long userId,
            PaymentRequest request,
            String idempotencyKey
    );

    PaymentResultResponse getPaymentResult(
            Long userId,
            Long paymentId
    );
}
