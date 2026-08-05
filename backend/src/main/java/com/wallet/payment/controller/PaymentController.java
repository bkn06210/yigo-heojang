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
import javax.servlet.http.HttpServletRequest;
import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/payments")
    public ResponseEntity<Map<String, Object>> processPayment(
            HttpServletRequest servletRequest,
            @RequestBody PaymentRequest request
    ) {
        Long userId = (Long) servletRequest.getAttribute(AUTHENTICATED_MEMBER_ID);

        PaymentProcessResponse data =
                paymentService.processPayment(userId, request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "寃곗젣媛 ?꾨즺?섏뿀?듬땲??");
        response.put("data", data);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/payments/{paymentId}")
    public Map<String, Object> getPaymentResult(
            HttpServletRequest request,
            @PathVariable("paymentId") Long paymentId
    ) {
        Long userId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        PaymentResultResponse data =
                paymentService.getPaymentResult(userId, paymentId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "寃곗젣 寃곌낵 議고쉶???깃났?덉뒿?덈떎.");
        response.put("data", data);

        return response;
    }
}
