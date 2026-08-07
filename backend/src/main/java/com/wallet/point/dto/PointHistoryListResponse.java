package com.wallet.point.dto;

import java.util.List;

public class PointHistoryListResponse {

    private List<PointHistoryResponse> histories;

    public PointHistoryListResponse(List<PointHistoryResponse> histories) {
        this.histories = histories;
    }

    public List<PointHistoryResponse> getHistories() {
        return histories;
    }

    public void setHistories(List<PointHistoryResponse> histories) {
        this.histories = histories;
    }
}
