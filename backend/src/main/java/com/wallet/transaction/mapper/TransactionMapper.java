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
            @Param("endDate") String endDate,
            // limit이 null이면 LIMIT 절이 붙지 않아 조건에 맞는 전체를 반환한다.
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    TransactionDetailResponse selectTransactionDetail(
            @Param("userId") Long userId,
            @Param("expenseId") Long expenseId
    );

    List<ExpenseCategoryResponse> selectExpenseCategories();
}
