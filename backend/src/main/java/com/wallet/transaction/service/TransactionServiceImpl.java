package com.wallet.transaction.service;

import com.wallet.transaction.dto.ExpenseCategoryListResponse;
import com.wallet.transaction.dto.ExpenseCategoryResponse;
import com.wallet.transaction.dto.TransactionDetailResponse;
import com.wallet.transaction.dto.TransactionListResponse;
import com.wallet.transaction.dto.TransactionResponse;
import com.wallet.transaction.dto.TransactionSummaryResponse;
import com.wallet.transaction.dto.TransactionSyncResponse;
import com.wallet.transaction.mapper.TransactionMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;

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
            String paymentStatus,
            String approvalStatus,
            String cardType,
            String region,
            String transactionType,
            String startDate,
            String endDate
    ) {
        validateFilter(paymentStatus, Set.of("APPROVED", "CANCELED"));
        validateFilter(approvalStatus, Set.of("APPROVED", "CONFIRMED"));
        validateFilter(cardType, Set.of("CREDIT", "CHECK", "PREPAID", "GIFT"));
        validateFilter(region, Set.of("DOMESTIC", "OVERSEAS"));
        validateFilter(transactionType, Set.of("LUMP_SUM", "INSTALLMENT", "CASH_ADVANCE"));
        validateDateRange(startDate, endDate);

        List<TransactionResponse> transactions =
                transactionMapper.selectTransactionList(
                        userId,
                        yearMonth,
                        categoryId,
                        userCardId,
                        paymentStatus,
                        approvalStatus,
                        cardType,
                        region,
                        transactionType,
                        startDate,
                        endDate
                );

        return new TransactionListResponse(transactions);
    }

    private void validateFilter(String value, Set<String> allowed) {
        if (value != null && !value.isBlank() && !allowed.contains(value)) {
            throw new BusinessException(ErrorCode.INPUT_INVALID);
        }
    }

    private void validateDateRange(String startDate, String endDate) {
        try {
            LocalDate start = startDate == null || startDate.isBlank() ? null : LocalDate.parse(startDate);
            LocalDate end = endDate == null || endDate.isBlank() ? null : LocalDate.parse(endDate);
            if (start != null && end != null && start.isAfter(end)) {
                throw new BusinessException(ErrorCode.INPUT_INVALID);
            }
        } catch (DateTimeParseException error) {
            throw new BusinessException(ErrorCode.INPUT_INVALID);
        }
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
    //프론트에서 추가했어요
    @Override
    public TransactionSummaryResponse getTransactionsSummary(
            Long userId,
            String yearMonth,
            Long userCardId
    ) {
        // 전체 거래 조회
        List<TransactionResponse> transactions =
                transactionMapper.selectTransactionList(
                        userId,
                        yearMonth,
                        null,
                        userCardId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        // 건수와 총액 계산
        int count = transactions.size();
        long totalAmount = transactions.stream()
                .mapToLong(TransactionResponse::getPaymentAmount)
                .sum();

        return new TransactionSummaryResponse(count, totalAmount);
    }

    @Override
    public TransactionSyncResponse syncTransactions(Long userId) {

        /*
         * 현재 실제 마이데이터 API가 없으므로 임시 구현이다.
         * 추후 실제 또는 Mock 거래 수집 로직이 생기면 여기에서
         * expense INSERT, 취소 반영 UPDATE, 혜택 엔진 호출을 연결한다.
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