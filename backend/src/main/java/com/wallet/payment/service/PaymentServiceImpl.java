package com.wallet.payment.service;

import com.wallet.payment.dto.PaymentCommand;
import com.wallet.engine.dto.PaymentSettlementResult;
import com.wallet.engine.dto.SettlementCommand;
import com.wallet.engine.service.SettlementService;
import com.wallet.payment.dto.PaymentProcessResponse;
import com.wallet.payment.dto.PaymentRequest;
import com.wallet.payment.dto.PaymentResultResponse;
import com.wallet.payment.mapper.PaymentMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final SettlementService settlementService;

    public PaymentServiceImpl(PaymentMapper paymentMapper, SettlementService settlementService) {
        this.paymentMapper = paymentMapper;
        this.settlementService = settlementService;
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
        command.setTransactionType(defaultValue(request.getTransactionType(), "LUMP_SUM"));
        command.setRegion(defaultValue(request.getRegion(), "DOMESTIC"));
        command.setInterestFreeYn("INSTALLMENT".equals(command.getTransactionType())
                ? defaultValue(request.getInterestFreeYn(), "N") : "N");

        paymentMapper.insertExpense(command);
        Long cardId = paymentMapper.selectCardIdByUserCardId(command.getUserCardId());
        PaymentSettlementResult settlement = settlementService.applyPayment(new SettlementCommand(
                command.getUserCardId(), cardId, command.getCategoryId(), command.getMerchantId(),
                command.getPaymentAmount(), command.getPaymentType(),
                "Y".equals(command.getInterestFreeYn()), LocalDateTime.now()));
        paymentMapper.updateExpenseSettlement(
                command.getExpenseId(), settlement.appliedBenefitId(), settlement.discountAmount());
        paymentMapper.insertPayment(command);

        PaymentProcessResponse response = new PaymentProcessResponse();
        response.setPaymentId(command.getPaymentId());
        response.setExpenseId(command.getExpenseId());
        response.setPaymentStatus("SUCCESS");
        response.setPaymentChannel("MOCK");
        response.setAppliedBenefitId(settlement.appliedBenefitId());
        response.setAppliedBenefitName(null);
        response.setDiscountAmount(settlement.discountAmount());
        response.setSavedPoint(0L);
        response.setTotalPoint(0L);
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
        validateEnum(request.getTransactionType(), Set.of("LUMP_SUM", "INSTALLMENT", "CASH_ADVANCE"), "transactionType");
        validateEnum(request.getRegion(), Set.of("DOMESTIC", "OVERSEAS"), "region");
        validateEnum(request.getInterestFreeYn(), Set.of("Y", "N"), "interestFreeYn");
    }

    private void validateEnum(String value, Set<String> allowed, String field) {
        if (value != null && !value.isBlank() && !allowed.contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 값이 올바르지 않습니다.");
        }
    }

    private String defaultValue(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }
}
