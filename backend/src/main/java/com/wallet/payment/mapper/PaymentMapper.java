package com.wallet.payment.mapper;

import com.wallet.payment.dto.PaymentCommand;
import com.wallet.payment.dto.PaymentIdempotencyRecord;
import com.wallet.payment.dto.PaymentPointWalletResponse;
import com.wallet.payment.dto.PaymentResultResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentMapper {

    int countUserCardByUserId(
            @Param("userId") Long userId,
            @Param("userCardId") Long userCardId
    );

    int countCategoryById(@Param("categoryId") Long categoryId);

    int insertExpense(PaymentCommand command);

    int insertPayment(PaymentCommand command);

    /** 멱등 키 선점. 같은 키가 이미 있으면 DuplicateKeyException, 처리 중이면 그 트랜잭션이 끝날 때까지 기다린다. */
    int insertIdempotencyKey(@Param("userId") Long userId,
                             @Param("idempotencyKey") String idempotencyKey,
                             @Param("requestHash") String requestHash);

    int updateIdempotencyPayment(@Param("userId") Long userId,
                                 @Param("idempotencyKey") String idempotencyKey,
                                 @Param("paymentId") Long paymentId);

    /** 선점에 실패한 키의 처리 결과. 잠금 읽기라 먼저 끝난 트랜잭션의 커밋을 본다. */
    PaymentIdempotencyRecord selectIdempotencyRecordForShare(@Param("userId") Long userId,
                                                             @Param("idempotencyKey") String idempotencyKey);

    Long selectCardIdByUserCardId(@Param("userCardId") Long userCardId);

    int updateExpenseSettlement(@Param("expenseId") Long expenseId,
                                @Param("appliedBenefitId") Long appliedBenefitId,
                                @Param("discountAmount") Long discountAmount);

    PaymentPointWalletResponse selectDefaultPointWallet(@Param("userId") Long userId);

    int updatePointWallet(
            @Param("pointWalletId") Long pointWalletId,
            @Param("savedPoint") Long savedPoint
    );

    int insertPointHistory(
            @Param("userId") Long userId,
            @Param("pointWalletId") Long pointWalletId,
            @Param("expenseId") Long expenseId,
            @Param("savedPoint") Long savedPoint,
            @Param("content") String content
    );

    PaymentResultResponse selectPaymentResult(
            @Param("userId") Long userId,
            @Param("paymentId") Long paymentId
    );
}
