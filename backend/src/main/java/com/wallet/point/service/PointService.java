package com.wallet.point.service;

import com.wallet.point.dto.PointHistoryListResponse;
import com.wallet.point.dto.PointListResponse;
import com.wallet.point.dto.PointUsagePlaceListResponse;

public interface PointService {

    PointListResponse getPointList(Long memberId);

    PointHistoryListResponse getPointHistory(
            Long memberId,
            Long pointWalletId,
            String pointType,
            String yearMonth
    );

    PointUsagePlaceListResponse getPointUsagePlaces(
            Long pointProviderId,
            Long categoryId
    );
}
