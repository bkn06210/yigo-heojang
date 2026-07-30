package com.wallet.point.dto;

import java.util.List;

public class PointUsagePlaceListResponse {

    private List<PointUsagePlaceResponse> usagePlaces;

    public PointUsagePlaceListResponse(List<PointUsagePlaceResponse> usagePlaces) {
        this.usagePlaces = usagePlaces;
    }

    public List<PointUsagePlaceResponse> getUsagePlaces() {
        return usagePlaces;
    }

    public void setUsagePlaces(List<PointUsagePlaceResponse> usagePlaces) {
        this.usagePlaces = usagePlaces;
    }
}