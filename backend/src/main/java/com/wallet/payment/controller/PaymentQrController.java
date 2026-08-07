package com.wallet.payment.controller;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

import java.time.format.DateTimeFormatter;
import javax.servlet.http.HttpServletRequest;

import com.wallet.common.ApiResponse;
import com.wallet.payment.dto.PaymentQrCreateRequest;
import com.wallet.payment.dto.PaymentQrCreateResponse;
import com.wallet.payment.dto.PaymentQrPayRequest;
import com.wallet.payment.dto.PaymentQrPayResponse;
import com.wallet.payment.dto.PaymentQrRecord;
import com.wallet.payment.dto.PaymentQrStatusResponse;
import com.wallet.payment.service.PaymentQrService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/qr")
public class PaymentQrController {

    private final PaymentQrService paymentQrService;

    public PaymentQrController(PaymentQrService paymentQrService) {
        this.paymentQrService = paymentQrService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentQrCreateResponse>> createPaymentQr(
            HttpServletRequest request,
            @RequestBody PaymentQrCreateRequest createRequest
    ) {
        Long memberId = authenticatedMemberId(request);
        PaymentQrCreateResponse data = paymentQrService.createPaymentQr(memberId, createRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("결제 QR 생성에 성공했습니다.", data));
    }

    @GetMapping("/{qrToken}")
    public ApiResponse<PaymentQrStatusResponse> getPaymentQr(
            HttpServletRequest request,
            @PathVariable String qrToken
    ) {
        Long memberId = authenticatedMemberId(request);
        PaymentQrRecord qr = paymentQrService.getPaymentQr(memberId, qrToken);
        PaymentQrStatusResponse data = new PaymentQrStatusResponse(
                qr.getQrToken(), qr.getStatus(),
                qr.getExpiresAt().toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                qr.getPaymentId());
        return ApiResponse.success("결제 QR 조회에 성공했습니다.", data);
    }

    @PostMapping("/{qrToken}/pay")
    public ApiResponse<PaymentQrPayResponse> payWithQr(
            HttpServletRequest request,
            @PathVariable String qrToken,
            @RequestBody PaymentQrPayRequest payRequest
    ) {
        Long memberId = authenticatedMemberId(request);
        PaymentQrPayResponse data = paymentQrService.payWithQr(memberId, qrToken, payRequest);
        return ApiResponse.success("QR 결제에 성공했습니다.", data);
    }

    private Long authenticatedMemberId(HttpServletRequest request) {
        return (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);
    }
}
