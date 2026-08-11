package com.wallet.transaction.dto;

public class TransactionSummaryResponse {

    private int count;
    private long totalAmount;

    public TransactionSummaryResponse(int count, long totalAmount) {
        this.count = count;
        this.totalAmount = totalAmount;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }
}
