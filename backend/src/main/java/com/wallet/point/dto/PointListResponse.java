package com.wallet.point.dto;

import java.util.List;

public class PointListResponse {

    private List<PointResponse> points;

    public PointListResponse(List<PointResponse> points) {
        this.points = points;
    }

    public List<PointResponse> getPoints() {
        return points;
    }

    public void setPoints(List<PointResponse> points) {
        this.points = points;
    }
}
