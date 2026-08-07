package com.wallet.payment.controller;

import com.wallet.common.ApiResponse;
import com.wallet.payment.dto.PaymentProcessResponse;
import com.wallet.payment.dto.PaymentRequest;
import com.wallet.payment.dto.PaymentResultResponse;
import com.wallet.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/payments")
    public ResponseEntity<ApiResponse<PaymentProcessResponse>> processPayment(
            HttpServletRequest servletRequest,
            @RequestBody PaymentRequest request
    ) {
        Long userId = (Long) servletRequest.getAttribute(AUTHENTICATED_MEMBER_ID);

        PaymentProcessResponse data =
                paymentService.processPayment(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("결제가 완료되었습니다.", data));
    }

    @GetMapping("/api/payments/{paymentId}")
    public ApiResponse<PaymentResultResponse> getPaymentResult(
            HttpServletRequest request,
            @PathVariable("paymentId") Long paymentId
    ) {
        Long userId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        PaymentResultResponse data =
                paymentService.getPaymentResult(userId, paymentId);

        return ApiResponse.success("결제 결과 조회에 성공했습니다.", data);
    }
}
