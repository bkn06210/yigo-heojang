package com.wallet.payment.service;

import com.wallet.payment.dto.PaymentProcessResponse;
import com.wallet.payment.dto.PaymentRequest;
import com.wallet.payment.dto.PaymentResultResponse;

public interface PaymentService {

    PaymentProcessResponse processPayment(
            Long userId,
            PaymentRequest request
    );

    PaymentResultResponse getPaymentResult(
            Long userId,
            Long paymentId
    );
}