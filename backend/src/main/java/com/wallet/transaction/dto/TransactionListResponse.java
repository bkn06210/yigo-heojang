package com.wallet.transaction.dto;

import java.util.List;

public class TransactionListResponse {

    private List<TransactionResponse> transactions;

    /**
     * 다음 페이지가 남아 있는지. page/size 없이 전체를 조회한 경우에는 항상 false다.
     *
     * 총 건수 대신 이 값만 내려주는 이유는, 무한 스크롤이 "더 있는가"만 알면 되기 때문이다.
     * 총 건수를 주려면 같은 조건으로 COUNT 쿼리를 한 번 더 돌려야 한다.
     */
    private boolean hasNext;

    public TransactionListResponse(List<TransactionResponse> transactions) {
        this(transactions, false);
    }

    public TransactionListResponse(List<TransactionResponse> transactions, boolean hasNext) {
        this.transactions = transactions;
        this.hasNext = hasNext;
    }

    public List<TransactionResponse> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionResponse> transactions) {
        this.transactions = transactions;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
}
