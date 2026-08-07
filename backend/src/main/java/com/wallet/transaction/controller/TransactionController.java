package com.wallet.transaction.controller;

import com.wallet.common.ApiResponse;
import com.wallet.transaction.dto.ExpenseCategoryListResponse;
import com.wallet.transaction.dto.TransactionDetailResponse;
import com.wallet.transaction.dto.TransactionListResponse;
import com.wallet.transaction.dto.TransactionSyncResponse;
import com.wallet.transaction.service.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

@RestController
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/api/transactions")
    public ApiResponse<TransactionListResponse> getTransactionList(
            @RequestParam(value = "yearMonth", required = false) String yearMonth,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "userCardId", required = false) Long userCardId,
            HttpServletRequest request,
            @RequestParam(value = "paymentStatus", required = false) String paymentStatus,
            @RequestParam(value = "approvalStatus", required = false) String approvalStatus,
            @RequestParam(value = "cardType", required = false) String cardType,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "transactionType", required = false) String transactionType,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        Long userId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        TransactionListResponse data =
                transactionService.getTransactionList(
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

        return ApiResponse.success("소비내역 목록 조회에 성공했습니다.", data);
    }

    @GetMapping("/api/transactions/{expenseId}")
    public ApiResponse<TransactionDetailResponse> getTransactionDetail(
            HttpServletRequest request,
            @PathVariable("expenseId") Long expenseId
    ) {
        Long userId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        TransactionDetailResponse data =
                transactionService.getTransactionDetail(userId, expenseId);

        return ApiResponse.success("소비내역 상세 조회에 성공했습니다.", data);
    }

    @PostMapping("/api/transactions/sync")
    public ApiResponse<TransactionSyncResponse> syncTransactions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        TransactionSyncResponse data =
                transactionService.syncTransactions(userId);

        return ApiResponse.success("거래 동기화가 완료되었습니다.", data);
    }

    @GetMapping("/api/expense-categories")
    public ApiResponse<ExpenseCategoryListResponse> getExpenseCategories() {
        ExpenseCategoryListResponse data =
                transactionService.getExpenseCategories();

        return ApiResponse.success("소비 카테고리 목록 조회에 성공했습니다.", data);
    }
}
