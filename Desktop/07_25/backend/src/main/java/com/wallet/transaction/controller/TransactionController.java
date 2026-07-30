package com.wallet.transaction.controller;

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

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/api/transactions")
    public Map<String, Object> getTransactionList(
            @RequestParam(value = "yearMonth", required = false) String yearMonth,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "userCardId", required = false) Long userCardId,
            @RequestParam(value = "paymentStatus", required = false) String paymentStatus
    ) {
        Long userId = 1L;

        TransactionListResponse data =
                transactionService.getTransactionList(
                        userId,
                        yearMonth,
                        categoryId,
                        userCardId,
                        paymentStatus
                );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "소비내역 목록 조회에 성공했습니다.");
        response.put("data", data);

        return response;
    }

    @GetMapping("/api/transactions/{expenseId}")
    public Map<String, Object> getTransactionDetail(
            @PathVariable("expenseId") Long expenseId
    ) {
        Long userId = 1L;

        TransactionDetailResponse data =
                transactionService.getTransactionDetail(userId, expenseId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "소비내역 상세 조회에 성공했습니다.");
        response.put("data", data);

        return response;
    }

    @PostMapping("/api/transactions/sync")
    public Map<String, Object> syncTransactions() {
        Long userId = 1L;

        TransactionSyncResponse data =
                transactionService.syncTransactions(userId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "거래 동기화가 완료되었습니다.");
        response.put("data", data);

        return response;
    }

    @GetMapping("/api/expense-categories")
    public Map<String, Object> getExpenseCategories() {
        ExpenseCategoryListResponse data =
                transactionService.getExpenseCategories();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "소비카테고리 목록 조회에 성공했습니다.");
        response.put("data", data);

        return response;
    }
}