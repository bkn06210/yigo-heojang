package com.wallet.payment.mapper;

import com.wallet.payment.dto.PaymentCommand;
import com.wallet.payment.dto.PaymentPointWalletResponse;
import com.wallet.payment.dto.PaymentResultResponse;
import org.apache.ibatis.annotations.Param;

public interface PaymentMapper {

    int countUserCardByUserId(
            @Param("userId") Long userId,
            @Param("userCardId") Long userCardId
    );

    int countCategoryById(@Param("categoryId") Long categoryId);

    int insertExpense(PaymentCommand command);

    int insertPayment(PaymentCommand command);

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