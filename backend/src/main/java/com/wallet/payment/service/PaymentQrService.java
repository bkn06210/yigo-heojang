package com.wallet.payment.service;

import com.wallet.payment.dto.PaymentQrCreateRequest;
import com.wallet.payment.dto.PaymentQrCreateResponse;
import com.wallet.payment.dto.PaymentQrInsertParam;
import com.wallet.payment.dto.PaymentQrPayRequest;
import com.wallet.payment.dto.PaymentQrPayResponse;
import com.wallet.payment.dto.PaymentQrRecord;
import com.wallet.payment.mapper.PaymentQrMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class PaymentQrService {

    private final PaymentQrMapper paymentQrMapper;
    private final PaymentQrPaymentExecutor paymentExecutor;

    public PaymentQrService(PaymentQrMapper paymentQrMapper, PaymentQrPaymentExecutor paymentExecutor) {
        this.paymentQrMapper = paymentQrMapper;
        this.paymentExecutor = paymentExecutor;
    }

    public PaymentQrCreateResponse createPaymentQr(
            Long memberId,
            PaymentQrCreateRequest request
    ) {
        if (request.getUserCardId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userCardId는 필수입니다.");
        }
        if (request.getPaymentAmount() == null || request.getPaymentAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentAmount는 0보다 커야 합니다.");
        }

        int cardCount = paymentQrMapper.countActiveUserCard(
                memberId,
                request.getUserCardId()
        );

        if (cardCount == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "사용 가능한 카드가 아닙니다.");
        }

        String qrToken = "QR_" + UUID.randomUUID()
                .toString()
                .replace("-", "");

        java.time.LocalDateTime expiresAt = java.time.LocalDateTime.now().plusMinutes(5);
        Timestamp expiresAtTimestamp = Timestamp.valueOf(expiresAt);

        PaymentQrInsertParam param = new PaymentQrInsertParam(
                qrToken,
                memberId,
                request.getUserCardId(),
                request.getPaymentAmount(),
                "READY",
                expiresAtTimestamp
        );

        paymentQrMapper.insertPaymentQr(param);

        return new PaymentQrCreateResponse(
                qrToken,
                expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    public PaymentQrRecord getPaymentQr(Long authenticatedMemberId, String qrToken) {
        PaymentQrRecord qr = paymentQrMapper.selectPaymentQrByToken(qrToken);

        if (qr == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 QR입니다.");
        }
        if (!qr.getMemberId().equals(authenticatedMemberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 QR만 조회할 수 있습니다.");
        }

        if ("READY".equals(qr.getStatus()) && isExpired(qr)) {
            paymentQrMapper.markQrExpired(qrToken);
            qr.setStatus("EXPIRED");
        }

        return qr;
    }

    public PaymentQrPayResponse payWithQr(
            Long authenticatedMemberId,
            String qrToken,
            PaymentQrPayRequest request
    ) {
        try {
            return paymentExecutor.execute(authenticatedMemberId, qrToken, request);
        } catch (RuntimeException exception) {
            paymentQrMapper.markQrFailed(qrToken, authenticatedMemberId, safeMessage(exception));
            throw exception;
        }
    }

    private String safeMessage(RuntimeException exception) {
        return exception instanceof ResponseStatusException
                ? ((ResponseStatusException) exception).getReason()
                : "결제 처리 중 오류가 발생했습니다.";
    }

    private boolean isExpired(PaymentQrRecord qr) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return qr.getExpiresAt().before(now);
    }


}
