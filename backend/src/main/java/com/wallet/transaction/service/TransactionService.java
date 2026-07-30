package com.wallet.transaction.service;

import com.wallet.transaction.dto.ExpenseCategoryListResponse;
import com.wallet.transaction.dto.TransactionDetailResponse;
import com.wallet.transaction.dto.TransactionListResponse;
import com.wallet.transaction.dto.TransactionSyncResponse;

import java.util.List;

public interface TransactionService {

    TransactionListResponse getTransactionList(
            Long userId,
            String yearMonth,
            Long categoryId,
            Long userCardId,
            String paymentStatus
    );

    TransactionDetailResponse getTransactionDetail(
            Long userId,
            Long expenseId
    );

    TransactionSyncResponse syncTransactions(Long userId);

    ExpenseCategoryListResponse getExpenseCategories();

    List<TransactionDetailResponse> getTransactionsByMemberId(Long memberId);
}