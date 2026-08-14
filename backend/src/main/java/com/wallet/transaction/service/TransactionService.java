package com.wallet.transaction.service;

import com.wallet.transaction.dto.ExpenseCategoryListResponse;
import com.wallet.transaction.dto.TransactionDetailResponse;
import com.wallet.transaction.dto.TransactionListResponse;
import com.wallet.transaction.dto.TransactionSummaryResponse;
import com.wallet.transaction.dto.TransactionSyncResponse;

import java.util.List;

public interface TransactionService {

    TransactionListResponse getTransactionList(
            Long userId,
            String yearMonth,
            Long categoryId,
            Long userCardId,
            String paymentStatus,
            String approvalStatus,
            String cardType,
            String region,
            String transactionType,
            String startDate,
            String endDate,
            // page/size가 null이면 페이지를 나누지 않고 조건에 맞는 전체를 반환한다(기존 동작).
            Integer page,
            Integer size
    );

    TransactionDetailResponse getTransactionDetail(
            Long userId,
            Long expenseId
    );

    TransactionSummaryResponse getTransactionsSummary(
            Long userId,
            String yearMonth,
            Long userCardId
    );

    TransactionSyncResponse syncTransactions(Long userId);

    ExpenseCategoryListResponse getExpenseCategories();

}