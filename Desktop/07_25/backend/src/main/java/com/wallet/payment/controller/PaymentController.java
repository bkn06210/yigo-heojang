package com.wallet.payment.controller;

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

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/payments")
    public ResponseEntity<Map<String, Object>> processPayment(
            @RequestBody PaymentRequest request
    ) {
        Long userId = 1L;

        PaymentProcessResponse data =
                paymentService.processPayment(userId, request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "결제가 완료되었습니다.");
        response.put("data", data);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/payments/{paymentId}")
    public Map<String, Object> getPaymentResult(
            @PathVariable("paymentId") Long paymentId
    ) {
        Long userId = 1L;

        PaymentResultResponse data =
                paymentService.getPaymentResult(userId, paymentId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "결제 결과 조회에 성공했습니다.");
        response.put("data", data);

        return response;
    }
}