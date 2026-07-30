package com.wallet.transaction.service;

import com.wallet.transaction.dto.ExpenseCategoryListResponse;
import com.wallet.transaction.dto.ExpenseCategoryResponse;
import com.wallet.transaction.dto.TransactionDetailResponse;
import com.wallet.transaction.dto.TransactionListResponse;
import com.wallet.transaction.dto.TransactionResponse;
import com.wallet.transaction.dto.TransactionSyncResponse;
import com.wallet.transaction.mapper.TransactionMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionMapper transactionMapper;

    public TransactionServiceImpl(TransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
    }

    @Override
    public TransactionListResponse getTransactionList(
            Long userId,
            String yearMonth,
            Long categoryId,
            Long userCardId,
            String paymentStatus
    ) {
        if (paymentStatus == null || paymentStatus.trim().isEmpty()) {
            paymentStatus = "APPROVED";
        }

        List<TransactionResponse> transactions =
                transactionMapper.selectTransactionList(
                        userId,
                        yearMonth,
                        categoryId,
                        userCardId,
                        paymentStatus
                );

        return new TransactionListResponse(transactions);
    }

    @Override
    public TransactionDetailResponse getTransactionDetail(
            Long userId,
            Long expenseId
    ) {
        TransactionDetailResponse detail =
                transactionMapper.selectTransactionDetail(userId, expenseId);

        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "소비내역을 찾을 수 없습니다.");
        }

        return detail;
    }

    @Override
    public TransactionSyncResponse syncTransactions(Long userId) {

        /*
         * 지금은 실제 마이데이터 API가 없으므로 임시 구현입니다.
         * 나중에 실제/Mock 거래 수집 로직이 생기면 여기에서
         * TBL_EXPENSE INSERT, 취소 반영 UPDATE, 혜택 엔진 호출을 연결하면 됩니다.
         */

        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        return new TransactionSyncResponse(now, 0, 0, 0);
    }

    @Override
    public ExpenseCategoryListResponse getExpenseCategories() {
        List<ExpenseCategoryResponse> categories =
                transactionMapper.selectExpenseCategories();

        return new ExpenseCategoryListResponse(categories);
    }
}