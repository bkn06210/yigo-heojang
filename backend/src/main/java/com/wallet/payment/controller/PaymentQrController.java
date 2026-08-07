package com.wallet.payment.controller;

import com.wallet.payment.dto.PaymentQrCreateRequest;
import com.wallet.payment.dto.PaymentQrPayRequest;
import com.wallet.payment.dto.PaymentQrRecord;
import com.wallet.payment.service.PaymentQrService;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

@RestController
public class PaymentQrController {

    private final PaymentQrService paymentQrService;

    public PaymentQrController(PaymentQrService paymentQrService) {
        this.paymentQrService = paymentQrService;
    }

    @PostMapping("/api/payments/qr")
    public Map<String, Object> createPaymentQr(
            HttpServletRequest servletRequest,
            @RequestBody PaymentQrCreateRequest request
    ) {
        try {
            Long memberId = (Long) servletRequest.getAttribute(AUTHENTICATED_MEMBER_ID);

            Object data = paymentQrService.createPaymentQr(memberId, request);

            return success(
                    "결제 QR 생성에 성공했습니다.",
                    data
            );
        } catch (IllegalArgumentException e) {
            return fail("BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return fail("ERROR", "결제 QR 생성 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/api/payments/qr/{qrToken}")
    public Map<String, Object> getPaymentQr(
            HttpServletRequest servletRequest,
            @PathVariable("qrToken") String qrToken
    ) {
        try {
            Long memberId = (Long) servletRequest.getAttribute(AUTHENTICATED_MEMBER_ID);
            PaymentQrRecord qr = paymentQrService.getPaymentQr(memberId, qrToken);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("qrToken", qr.getQrToken());
            data.put("status", qr.getStatus());
            data.put("expiresAt", qr.getExpiresAt().toLocalDateTime()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            data.put("paymentId", qr.getPaymentId());

            return success("결제 QR 조회에 성공했습니다.", data);
        } catch (IllegalArgumentException e) {
            return fail("BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return fail("ERROR", "결제 QR 조회 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/api/payments/qr/{qrToken}/pay")
    public Map<String, Object> payWithQr(
            HttpServletRequest servletRequest,
            @PathVariable("qrToken") String qrToken,
            @RequestBody PaymentQrPayRequest request
    ) {
        try {
            Long memberId = (Long) servletRequest.getAttribute(AUTHENTICATED_MEMBER_ID);
            Object data = paymentQrService.payWithQr(memberId, qrToken, request);

            return success(
                    "QR 결제에 성공했습니다.",
                    data
            );
        } catch (IllegalArgumentException e) {
            return fail("BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return fail("ERROR", "QR 결제 중 오류가 발생했습니다.");
        }
    }

    private Map<String, Object> success(String message, Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    private Map<String, Object> fail(String code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("code", code);
        response.put("message", message);
        response.put("data", null);
        return response;
    }
}
