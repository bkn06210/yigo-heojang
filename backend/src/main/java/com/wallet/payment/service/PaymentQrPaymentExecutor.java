package com.wallet.payment.service;

import java.time.LocalDateTime;

import com.wallet.engine.dto.PaymentSettlementResult;
import com.wallet.engine.dto.SettlementCommand;
import com.wallet.engine.service.SettlementService;
import com.wallet.payment.dto.PaymentQrPayParam;
import com.wallet.payment.dto.PaymentQrPayRequest;
import com.wallet.payment.dto.PaymentQrPayResponse;
import com.wallet.payment.dto.PaymentQrRecord;
import com.wallet.payment.mapper.PaymentQrMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentQrPaymentExecutor {

    private final PaymentQrMapper paymentQrMapper;
    private final SettlementService settlementService;

    public PaymentQrPaymentExecutor(PaymentQrMapper paymentQrMapper, SettlementService settlementService) {
        this.paymentQrMapper = paymentQrMapper;
        this.settlementService = settlementService;
    }

    @Transactional
    public PaymentQrPayResponse execute(Long memberId, String qrToken, PaymentQrPayRequest request) {
        validate(request);
        PaymentQrRecord qr = paymentQrMapper.selectPaymentQrForUpdate(qrToken);
        if (qr == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 QR입니다.");
        }
        if (!qr.getMemberId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 QR만 결제할 수 있습니다.");
        }
        if (!"READY".equals(qr.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용했거나 사용할 수 없는 QR입니다.");
        }
        if (qr.getExpiresAt().before(new java.sql.Timestamp(System.currentTimeMillis()))) {
            throw new ResponseStatusException(HttpStatus.GONE, "만료된 QR입니다.");
        }

        PaymentQrPayParam payParam = new PaymentQrPayParam();
        payParam.setMemberId(qr.getMemberId());
        payParam.setUserCardId(qr.getUserCardId());
        payParam.setMerchantId(request.getMerchantId());
        payParam.setMerchantName(request.getMerchantName());
        payParam.setCategoryId(request.getCategoryId());
        payParam.setPaymentAmount(qr.getPaymentAmount());

        paymentQrMapper.insertExpenseByQr(payParam);
        Long cardId = paymentQrMapper.selectCardIdByUserCardId(qr.getUserCardId());
        PaymentSettlementResult settlement = settlementService.applyPayment(new SettlementCommand(
                qr.getUserCardId(), cardId, request.getCategoryId(), request.getMerchantId(),
                qr.getPaymentAmount(), "CARD", false, LocalDateTime.now()));
        paymentQrMapper.updateExpenseSettlement(
                payParam.getExpenseId(), settlement.appliedBenefitId(), settlement.discountAmount());
        paymentQrMapper.insertPaymentByQr(payParam);
        paymentQrMapper.markQrUsed(qrToken, payParam.getPaymentId());

        return new PaymentQrPayResponse(
                payParam.getPaymentId(), payParam.getExpenseId(), "SUCCESS", "결제가 완료되었습니다.");
    }

    private void validate(PaymentQrPayRequest request) {
        if (request == null || request.getMerchantName() == null || request.getMerchantName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "merchantName은 필수입니다.");
        }
        if (request.getCategoryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryId는 필수입니다.");
        }
    }
}
