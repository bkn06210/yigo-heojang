package com.wallet.point.service;

import com.wallet.point.dto.PointHistoryListResponse;
import com.wallet.point.dto.PointHistoryResponse;
import com.wallet.point.dto.PointListResponse;
import com.wallet.point.dto.PointResponse;
import com.wallet.point.dto.PointUsagePlaceListResponse;
import com.wallet.point.dto.PointUsagePlaceResponse;
import com.wallet.point.mapper.PointMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointServiceImpl implements PointService {

    private final PointMapper pointMapper;

    public PointServiceImpl(PointMapper pointMapper) {
        this.pointMapper = pointMapper;
    }

    @Override
    public PointListResponse getPointList(Long memberId) {
        List<PointResponse> points = pointMapper.selectPointList(memberId);
        return new PointListResponse(points);
    }

    @Override
    public PointHistoryListResponse getPointHistory(
            Long memberId,
            Long pointWalletId,
            String pointType,
            String yearMonth
    ) {
        List<PointHistoryResponse> histories =
                pointMapper.selectPointHistory(memberId, pointWalletId, pointType, yearMonth);

        return new PointHistoryListResponse(histories);
    }

    @Override
    public PointUsagePlaceListResponse getPointUsagePlaces(
            Long pointProviderId,
            Long categoryId
    ) {
        List<PointUsagePlaceResponse> usagePlaces =
                pointMapper.selectPointUsagePlaces(pointProviderId, categoryId);

        return new PointUsagePlaceListResponse(usagePlaces);
    }
}