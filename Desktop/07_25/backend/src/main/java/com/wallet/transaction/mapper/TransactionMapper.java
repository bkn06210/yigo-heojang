package com.wallet.transaction.mapper;

import com.wallet.transaction.dto.ExpenseCategoryResponse;
import com.wallet.transaction.dto.TransactionDetailResponse;
import com.wallet.transaction.dto.TransactionResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TransactionMapper {

    List<TransactionResponse> selectTransactionList(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth,
            @Param("categoryId") Long categoryId,
            @Param("userCardId") Long userCardId,
            @Param("paymentStatus") String paymentStatus
    );

    TransactionDetailResponse selectTransactionDetail(
            @Param("userId") Long userId,
            @Param("expenseId") Long expenseId
    );

    List<ExpenseCategoryResponse> selectExpenseCategories();
}