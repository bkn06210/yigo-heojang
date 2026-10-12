package com.wallet.payment.service;

import com.wallet.common.util.Sha256Hasher;
import com.wallet.payment.dto.PaymentCommand;
import com.wallet.payment.dto.PaymentIdempotencyRecord;
import com.wallet.engine.dto.PaymentSettlementResult;
import com.wallet.engine.dto.SettlementCommand;
import com.wallet.engine.service.SettlementService;
import com.wallet.payment.dto.PaymentProcessResponse;
import com.wallet.payment.dto.PaymentRequest;
import com.wallet.payment.dto.PaymentResultResponse;
import com.wallet.payment.mapper.PaymentMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 64;

    private final PaymentMapper paymentMapper;
    private final SettlementService settlementService;
    private final Sha256Hasher sha256Hasher;

    public PaymentServiceImpl(PaymentMapper paymentMapper, SettlementService settlementService,
                              Sha256Hasher sha256Hasher) {
        this.paymentMapper = paymentMapper;
        this.settlementService = settlementService;
        this.sha256Hasher = sha256Hasher;
    }

    @Override
    @Transactional
    public PaymentProcessResponse processPayment(
            Long userId,
            PaymentRequest request,
            String idempotencyKey
    ) {
        validateRequest(request);
        validateIdempotencyKey(idempotencyKey);
        PaymentCommand command = toCommand(userId, request);

        // 키 선점은 이 트랜잭션의 첫 DB 접근이어야 한다. 앞에서 일반 SELECT를 하면 REPEATABLE READ
        // 스냅샷이 그때 고정돼, 기다린 끝에 커밋된 앞 결제를 재응답용으로 읽지 못한다
        String requestHash = null;
        if (idempotencyKey != null) {
            requestHash = requestHashOf(command);
            PaymentProcessResponse replayed = claimIdempotencyKey(userId, idempotencyKey, requestHash);
            if (replayed != null) {
                return replayed;
            }
        }

        int cardCount = paymentMapper.countUserCardByUserId(userId, request.getUserCardId());
        if (cardCount == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 카드가 아닙니다.");
        }

        int categoryCount = paymentMapper.countCategoryById(request.getCategoryId());
        if (categoryCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다.");
        }

        paymentMapper.insertExpense(command);
        Long cardId = paymentMapper.selectCardIdByUserCardId(command.getUserCardId());
        PaymentSettlementResult settlement = settlementService.applyPayment(new SettlementCommand(
                command.getUserCardId(), cardId, command.getCategoryId(), command.getMerchantId(),
                command.getPaymentAmount(), command.getPaymentType(),
                "Y".equals(command.getInterestFreeYn()), LocalDateTime.now()));
        paymentMapper.updateExpenseSettlement(
                command.getExpenseId(), settlement.appliedBenefitId(), settlement.discountAmount());
        paymentMapper.insertPayment(command);
        if (idempotencyKey != null) {
            paymentMapper.updateIdempotencyPayment(userId, idempotencyKey, command.getPaymentId());
        }

        return toResponse(userId, command.getPaymentId(), command.getExpenseId(),
                settlement.appliedBenefitId(), settlement.discountAmount());
    }

    /**
     * 멱등 키를 선점한다. 처음 보는 키면 null을 돌려 결제를 진행시키고, 이미 처리된 키면 그 결과를 돌려준다.
     * 같은 키가 처리 중이면 INSERT가 PK 잠금에서 기다리므로, 여기서 중복 키를 받았다면 앞 결제는 이미 커밋됐다.
     */
    private PaymentProcessResponse claimIdempotencyKey(Long userId, String idempotencyKey, String requestHash) {
        try {
            paymentMapper.insertIdempotencyKey(userId, idempotencyKey, requestHash);
            return null;
        } catch (DuplicateKeyException alreadyUsed) {
            PaymentIdempotencyRecord record = paymentMapper.selectIdempotencyRecordForShare(userId, idempotencyKey);
            if (!requestHash.equals(record.getRequestHash())) {
                // 재시도가 아니라 다른 결제에 키를 재사용한 것 — 처음 결과를 돌려주면 요청과 다른 결제를 성공으로 알린다
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "같은 Idempotency-Key로 다른 결제를 요청했습니다.");
            }
            if (record.getPaymentId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "같은 결제 요청을 처리하고 있습니다.");
            }
            return toResponse(userId, record.getPaymentId(), record.getExpenseId(),
                    record.getAppliedBenefitId(), record.getDiscountAmount());
        }
    }

    /** 요청 내용의 지문. 기본값을 채운 뒤의 값으로 만들어, 생략한 필드와 기본값을 적은 필드를 같은 요청으로 본다. */
    private String requestHashOf(PaymentCommand command) {
        return sha256Hasher.sha256(String.join("|",
                String.valueOf(command.getUserCardId()),
                String.valueOf(command.getCategoryId()),
                String.valueOf(command.getMerchantId()),
                command.getMerchantName(),
                String.valueOf(command.getPaymentAmount()),
                command.getPaymentType(),
                command.getTransactionType(),
                command.getRegion(),
                command.getInterestFreeYn(),
                command.getIsRecommendBased()));
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return;
        }
        if (idempotencyKey.isBlank() || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key는 1~" + MAX_IDEMPOTENCY_KEY_LENGTH + "자여야 합니다.");
        }
    }

    private PaymentCommand toCommand(Long userId, PaymentRequest request) {
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
        return command;
    }

    private PaymentProcessResponse toResponse(Long userId, Long paymentId, Long expenseId,
                                              Long appliedBenefitId, Long discountAmount) {
        PaymentProcessResponse response = new PaymentProcessResponse();
        response.setPaymentId(paymentId);
        response.setExpenseId(expenseId);
        response.setPaymentStatus("SUCCESS");
        response.setPaymentChannel("MOCK");
        response.setAppliedBenefitId(appliedBenefitId);
        response.setAppliedBenefitName(null);
        response.setDiscountAmount(discountAmount);
        response.setSavedPoint(0L);
        response.setTotalPoint(0L);
        response.setCompletedAt(null);

        PaymentResultResponse result =
                paymentMapper.selectPaymentResult(userId, paymentId);

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
