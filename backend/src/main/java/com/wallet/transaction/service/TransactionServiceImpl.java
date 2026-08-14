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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;

@Service
public class TransactionServiceImpl implements TransactionService {

    // 한 번에 가져갈 수 있는 최대 건수. size=100000 같은 요청으로 DB에 부담을 주지 못하게 막는다.
    private static final int MAX_PAGE_SIZE = 100;

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
            String endDate,
            Integer page,
            Integer size
    ) {
        validateFilter(paymentStatus, Set.of("APPROVED", "CANCELED"));
        validateFilter(approvalStatus, Set.of("APPROVED", "CONFIRMED"));
        validateFilter(cardType, Set.of("CREDIT", "CHECK", "PREPAID", "GIFT"));
        validateFilter(region, Set.of("DOMESTIC", "OVERSEAS"));
        validateFilter(transactionType, Set.of("LUMP_SUM", "INSTALLMENT", "CASH_ADVANCE"));
        validateDateRange(startDate, endDate);
        validatePageRequest(page, size);

        boolean paged = size != null;

        // 다음 페이지가 있는지 알기 위해 요청한 개수보다 한 건 더 읽는다.
        // 별도 COUNT 쿼리를 한 번 더 돌리는 것보다 싸고, 무한 스크롤에는 총 개수가 필요 없다.
        Integer limit = paged ? size + 1 : null;
        Integer offset = paged ? (page == null ? 0 : page) * size : null;

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
                        endDate,
                        limit,
                        offset
                );

        boolean hasNext = paged && transactions.size() > size;
        if (hasNext) {
            // 존재 여부를 확인하려고 더 읽은 한 건은 응답에서 덜어낸다.
            transactions = new ArrayList<>(transactions.subList(0, size));
        }

        return new TransactionListResponse(transactions, hasNext);
    }

    /**
     * page/size는 둘 다 없거나(전체 조회) 유효한 값이어야 한다.
     * size 없이 page만 보내는 것은 의도를 알 수 없어 거부한다 — 조용히 전체를 반환하면
     * 호출한 쪽은 페이지를 나눠 받았다고 착각한다.
     */
    private void validatePageRequest(Integer page, Integer size) {
        if (size == null) {
            if (page != null) {
                throw new BusinessException(ErrorCode.QUERY_PARAMETER_INVALID);
            }
            return;
        }
        if (size < 1 || size > MAX_PAGE_SIZE || (page != null && page < 0)) {
            throw new BusinessException(ErrorCode.QUERY_PARAMETER_INVALID);
        }
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
        // 전체 거래 조회 — 합계를 내야 하므로 페이지를 나누지 않는다(limit/offset null).
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