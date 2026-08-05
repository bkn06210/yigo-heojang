package com.wallet.transaction.mapper;

import com.wallet.transaction.dto.ExpenseCategoryResponse;
import com.wallet.transaction.dto.TransactionDetailResponse;
import com.wallet.transaction.dto.TransactionResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TransactionMapper {

    List<TransactionResponse> selectTransactionList(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth,
            @Param("categoryId") Long categoryId,
            @Param("userCardId") Long userCardId,
            @Param("paymentStatus") String paymentStatus,
            @Param("approvalStatus") String approvalStatus,
            @Param("cardType") String cardType,
            @Param("region") String region,
            @Param("transactionType") String transactionType,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    TransactionDetailResponse selectTransactionDetail(
            @Param("userId") Long userId,
            @Param("expenseId") Long expenseId
    );

    List<TransactionDetailResponse> selectTransactionsByMemberId(
            @Param("memberId") Long memberId
    );

    List<ExpenseCategoryResponse> selectExpenseCategories();
}
