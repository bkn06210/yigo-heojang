package com.wallet.payment.service;

import com.wallet.payment.dto.PaymentCommand;
import com.wallet.payment.dto.PaymentPointWalletResponse;
import com.wallet.payment.dto.PaymentProcessResponse;
import com.wallet.payment.dto.PaymentRequest;
import com.wallet.payment.dto.PaymentResultResponse;
import com.wallet.payment.mapper.PaymentMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;

    public PaymentServiceImpl(PaymentMapper paymentMapper) {
        this.paymentMapper = paymentMapper;
    }

    @Override
    @Transactional
    public PaymentProcessResponse processPayment(
            Long userId,
            PaymentRequest request
    ) {
        validateRequest(request);

        int cardCount = paymentMapper.countUserCardByUserId(userId, request.getUserCardId());
        if (cardCount == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 카드가 아닙니다.");
        }

        int categoryCount = paymentMapper.countCategoryById(request.getCategoryId());
        if (categoryCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다.");
        }

        PaymentCommand command = new PaymentCommand();
        command.setUserId(userId);
        command.setUserCardId(request.getUserCardId());
        command.setCategoryId(request.getCategoryId());
        command.setMerchantId(request.getMerchantId());
        command.setMerchantName(request.getMerchantName());
        command.setPaymentAmount(request.getPaymentAmount());
        command.setPaymentStatus("SUCCESS");
        command.setPaymentChannel("MOCK");
        command.setIsRecommendBased(Boolean.TRUE.equals(request.getIsRecommendBased()) ? "Y" : "N");
        command.setPaymentType(defaultValue(request.getPaymentType(), "CARD"));
        command.setInterestFreeYn(defaultValue(request.getInterestFreeYn(), "N"));

        /*
         * 1. 결제 성공 시 소비내역 생성
         * 2. 결제 이력 저장
         * 3. 포인트 임시 적립
         *
         * 혜택 엔진 연동 전이므로 appliedBenefitId, discountAmount는 아직 0/null 처리.
         */
        paymentMapper.insertExpense(command);
        paymentMapper.insertPayment(command);

        Long savedPoint = calculateSavedPoint(request.getPaymentAmount());
        Long totalPoint = 0L;

        PaymentPointWalletResponse wallet = paymentMapper.selectDefaultPointWallet(userId);

        if (wallet != null && savedPoint > 0) {
            paymentMapper.updatePointWallet(wallet.getPointWalletId(), savedPoint);

            String content = command.getMerchantName() + " 결제로 포인트 적립";
            paymentMapper.insertPointHistory(
                    userId,
                    wallet.getPointWalletId(),
                    command.getExpenseId(),
                    savedPoint,
                    content
            );

            totalPoint = wallet.getTotalPoint() + savedPoint;
        }

        PaymentProcessResponse response = new PaymentProcessResponse();
        response.setPaymentId(command.getPaymentId());
        response.setExpenseId(command.getExpenseId());
        response.setPaymentStatus("SUCCESS");
        response.setPaymentChannel("MOCK");
        response.setAppliedBenefitId(null);
        response.setAppliedBenefitName(null);
        response.setDiscountAmount(0L);
        response.setSavedPoint(savedPoint);
        response.setTotalPoint(totalPoint);
        response.setCompletedAt(null);

        PaymentResultResponse result =
                paymentMapper.selectPaymentResult(userId, command.getPaymentId());

        if (result != null) {
            response.setCompletedAt(result.getCompletedAt());
            response.setSavedPoint(result.getSavedPoint());
            response.setTotalPoint(result.getTotalPoint());
        }

        return response;
    }

    @Override
    public PaymentResultResponse getPaymentResult(
            Long userId,
            Long paymentId
    ) {
        PaymentResultResponse result =
                paymentMapper.selectPaymentResult(userId, paymentId);

        if (result == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다.");
        }

        return result;
    }

    private void validateRequest(PaymentRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 본문이 필요합니다.");
        }

        if (request.getUserCardId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userCardId는 필수입니다.");
        }

        if (request.getCategoryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryId는 필수입니다.");
        }

        if (request.getPaymentAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentAmount는 필수입니다.");
        }

        if (request.getPaymentAmount() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제금액은 1원 이상이어야 합니다.");
        }

        if (request.getMerchantName() == null || request.getMerchantName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "merchantName은 필수입니다.");
        }
    }

    private Long calculateSavedPoint(Long paymentAmount) {
        /*
         * 임시 정책:
         * 혜택/포인트 엔진 연결 전까지 결제금액의 1%를 적립 처리.
         * 예: 10,000원 결제 → 100포인트
         */
        return paymentAmount / 100;
    }

    private String defaultValue(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }
}