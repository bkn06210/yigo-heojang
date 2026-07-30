package com.wallet.payment.service;




import com.wallet.payment.dto.*;
import com.wallet.payment.mapper.PaymentQrMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.UUID;

@Service
public class PaymentQrService {

    private final PaymentQrMapper paymentQrMapper;

    public PaymentQrService(PaymentQrMapper paymentQrMapper) {
        this.paymentQrMapper = paymentQrMapper;
    }

    public PaymentQrCreateResponse createPaymentQr(
            Long memberId,
            PaymentQrCreateRequest request
    ) {
        if (request.getUserCardId() == null) {
            throw new IllegalArgumentException("userCardId는 필수입니다.");
        }

        int cardCount = paymentQrMapper.countActiveUserCard(
                memberId,
                request.getUserCardId()
        );

        if (cardCount == 0) {
            throw new IllegalArgumentException("사용 가능한 카드가 아닙니다.");
        }

        String qrToken = "QR_" + UUID.randomUUID()
                .toString()
                .replace("-", "");

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);
        Timestamp expiresAtTimestamp = Timestamp.valueOf(expiresAt);

        PaymentQrInsertParam param = new PaymentQrInsertParam(
                qrToken,
                memberId,
                request.getUserCardId(),
                "READY",
                expiresAtTimestamp
        );

        paymentQrMapper.insertPaymentQr(param);

        String qrImageBase64 = "";

        return new PaymentQrCreateResponse(
                qrToken,
                qrImageBase64,
                expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    public PaymentQrRecord getPaymentQr(String qrToken) {
        PaymentQrRecord qr = paymentQrMapper.selectPaymentQrByToken(qrToken);

        if (qr == null) {
            throw new IllegalArgumentException("존재하지 않는 QR입니다.");
        }

        if ("READY".equals(qr.getStatus()) && isExpired(qr)) {
            paymentQrMapper.markQrExpired(qrToken);
            qr.setStatus("EXPIRED");
        }

        return qr;
    }

    @Transactional
    public PaymentQrPayResponse payWithQr(
            String qrToken,
            PaymentQrPayRequest request
    ) {
        try {
            validatePayRequest(request);

            PaymentQrRecord qr = paymentQrMapper.selectPaymentQrForUpdate(qrToken);

            if (qr == null) {
                return new PaymentQrPayResponse(
                        null,
                        null,
                        "FAILED",
                        "존재하지 않는 QR입니다."
                );
            }

            if (!"READY".equals(qr.getStatus())) {
                return new PaymentQrPayResponse(
                        null,
                        null,
                        "FAILED",
                        "이미 사용되었거나 사용할 수 없는 QR입니다."
                );
            }

            if (isExpired(qr)) {
                paymentQrMapper.markQrExpired(qrToken);

                return new PaymentQrPayResponse(
                        null,
                        null,
                        "FAILED",
                        "만료된 QR입니다."
                );
            }

            PaymentQrPayParam payParam = new PaymentQrPayParam();
            payParam.setMemberId(qr.getMemberId());
            payParam.setUserCardId(qr.getUserCardId());
            payParam.setMerchantId(request.getMerchantId());
            payParam.setMerchantName(request.getMerchantName());
            payParam.setCategoryId(request.getCategoryId());
            payParam.setPaymentAmount(request.getPaymentAmount());

            paymentQrMapper.insertExpenseByQr(payParam);
            paymentQrMapper.insertPaymentByQr(payParam);

            paymentQrMapper.markQrUsed(qrToken, payParam.getPaymentId());

            return new PaymentQrPayResponse(
                    payParam.getPaymentId(),
                    payParam.getExpenseId(),
                    "SUCCESS",
                    "결제가 완료되었습니다."
            );
        } catch (IllegalArgumentException e) {
            paymentQrMapper.markQrFailed(qrToken, e.getMessage());

            return new PaymentQrPayResponse(
                    null,
                    null,
                    "FAILED",
                    e.getMessage()
            );
        }
    }

    private void validatePayRequest(PaymentQrPayRequest request) {
        if (request.getMerchantName() == null || request.getMerchantName().trim().isEmpty()) {
            throw new IllegalArgumentException("merchantName은 필수입니다.");
        }

        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("categoryId는 필수입니다.");
        }

        if (request.getPaymentAmount() == null || request.getPaymentAmount() <= 0) {
            throw new IllegalArgumentException("paymentAmount는 0보다 커야 합니다.");
        }
    }

    private boolean isExpired(PaymentQrRecord qr) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return qr.getExpiresAt().before(now);
    }


}