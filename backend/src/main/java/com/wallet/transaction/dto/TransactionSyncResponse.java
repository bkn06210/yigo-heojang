package com.wallet.transaction.dto;

public class TransactionSyncResponse {

    private String syncedAt;
    private int addedCount;
    private int canceledCount;
    private int failedCount;

    public TransactionSyncResponse(String syncedAt, int addedCount, int canceledCount, int failedCount) {
        this.syncedAt = syncedAt;
        this.addedCount = addedCount;
        this.canceledCount = canceledCount;
        this.failedCount = failedCount;
    }

    public String getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(String syncedAt) {
        this.syncedAt = syncedAt;
    }

    public int getAddedCount() {
        return addedCount;
    }

    public void setAddedCount(int addedCount) {
        this.addedCount = addedCount;
    }

    public int getCanceledCount() {
        return canceledCount;
    }

    public void setCanceledCount(int canceledCount) {
        this.canceledCount = canceledCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }
}
