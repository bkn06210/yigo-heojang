package com.wallet.payment.service;

import java.time.LocalDateTime;
import java.util.Objects;

import com.wallet.engine.dto.PaymentSettlementResult;
import com.wallet.engine.dto.SettlementCommand;
import com.wallet.engine.service.SettlementService;
import com.wallet.payment.dto.PaymentQrPaidRecord;
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
        if ("USED".equals(qr.getStatus()) && qr.getPaymentId() != null) {
            return replayOrReject(qr.getPaymentId(), request);
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

    /**
     * 이미 결제된 QR로 다시 온 요청. QR 토큰이 곧 멱등 키다.
     * 응답을 못 받은 클라이언트의 재시도면 처음 결과를 그대로 돌려준다 — 409를 주면 결제는 됐는데
     * 화면에는 실패로 보인다. 가맹점·카테고리가 다르면 재시도가 아니라 다른 결제라 거절한다.
     * QR 행을 FOR UPDATE로 잡은 뒤 읽으므로, 동시에 온 같은 요청도 먼저 끝난 결제를 본다.
     */
    private PaymentQrPayResponse replayOrReject(Long paymentId, PaymentQrPayRequest request) {
        PaymentQrPaidRecord paid = paymentQrMapper.selectPaidRecord(paymentId);
        boolean sameRequest = paid != null
                && Objects.equals(paid.getMerchantId(), request.getMerchantId())
                && Objects.equals(paid.getMerchantName(), request.getMerchantName())
                && Objects.equals(paid.getCategoryId(), request.getCategoryId());
        if (!sameRequest) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용했거나 사용할 수 없는 QR입니다.");
        }
        return new PaymentQrPayResponse(paid.getPaymentId(), paid.getExpenseId(), "SUCCESS", "결제가 완료되었습니다.");
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
