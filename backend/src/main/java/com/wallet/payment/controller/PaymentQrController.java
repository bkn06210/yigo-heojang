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
                    "寃곗젣 QR ?앹꽦???깃났?덉뒿?덈떎.",
                    data
            );
        } catch (IllegalArgumentException e) {
            return fail("BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return fail("ERROR", "寃곗젣 QR ?앹꽦 以??ㅻ쪟媛 諛쒖깮?덉뒿?덈떎.");
        }
    }

    @GetMapping("/api/payments/qr/{qrToken}")
    public Map<String, Object> getPaymentQr(
            @PathVariable("qrToken") String qrToken
    ) {
        try {
            PaymentQrRecord qr = paymentQrService.getPaymentQr(qrToken);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("qrToken", qr.getQrToken());
            data.put("memberId", qr.getMemberId());
            data.put("userCardId", qr.getUserCardId());
            data.put("status", qr.getStatus());
            data.put("expiresAt", qr.getExpiresAt().toLocalDateTime()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            data.put("paymentId", qr.getPaymentId());

            return success("寃곗젣 QR 議고쉶???깃났?덉뒿?덈떎.", data);
        } catch (IllegalArgumentException e) {
            return fail("BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return fail("ERROR", "寃곗젣 QR 議고쉶 以??ㅻ쪟媛 諛쒖깮?덉뒿?덈떎.");
        }
    }

    @PostMapping("/api/payments/qr/{qrToken}/pay")
    public Map<String, Object> payWithQr(
            @PathVariable("qrToken") String qrToken,
            @RequestBody PaymentQrPayRequest request
    ) {
        try {
            Object data = paymentQrService.payWithQr(qrToken, request);

            return success(
                    "QR 寃곗젣???깃났?덉뒿?덈떎.",
                    data
            );
        } catch (IllegalArgumentException e) {
            return fail("BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return fail("ERROR", "QR 寃곗젣 以??ㅻ쪟媛 諛쒖깮?덉뒿?덈떎.");
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
