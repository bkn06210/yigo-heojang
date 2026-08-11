package com.wallet.point.mapper;

import com.wallet.point.dto.PointHistoryResponse;
import com.wallet.point.dto.PointResponse;
import com.wallet.point.dto.PointUsagePlaceResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PointMapper {

    List<PointResponse> selectPointList(@Param("memberId") Long memberId);

    List<PointHistoryResponse> selectPointHistory(
            @Param("memberId") Long memberId,
            @Param("pointWalletId") Long pointWalletId,
            @Param("pointType") String pointType,
            @Param("yearMonth") String yearMonth
    );

    List<PointUsagePlaceResponse> selectPointUsagePlaces(
            @Param("pointProviderId") Long pointProviderId,
            @Param("categoryId") Long categoryId
    );
}
