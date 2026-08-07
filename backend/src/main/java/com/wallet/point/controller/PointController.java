package com.wallet.point.controller;

import com.wallet.common.ApiResponse;
import com.wallet.point.dto.PointHistoryListResponse;
import com.wallet.point.dto.PointListResponse;
import com.wallet.point.dto.PointUsagePlaceListResponse;
import com.wallet.point.service.PointService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

@RestController
public class PointController {

    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    @GetMapping("/api/points")
    public ApiResponse<PointListResponse> getPointList(HttpServletRequest request) {

        // JWT 인증 필터가 요청 속성에 설정한 회원 ID를 사용한다.
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        PointListResponse data = pointService.getPointList(memberId);

        return ApiResponse.success("포인트 목록 조회에 성공했습니다.", data);
    }

    @GetMapping("/api/points/history")
    public ApiResponse<PointHistoryListResponse> getPointHistory(
            @RequestParam(value = "pointWalletId", required = false) Long pointWalletId,
            @RequestParam(value = "pointType", required = false) String pointType,
            HttpServletRequest request,
            @RequestParam(value = "yearMonth", required = false) String yearMonth
    ) {
        // JWT 인증 필터가 요청 속성에 설정한 회원 ID를 사용한다.
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        PointHistoryListResponse data =
                pointService.getPointHistory(memberId, pointWalletId, pointType, yearMonth);

        return ApiResponse.success("포인트 내역 조회에 성공했습니다.", data);
    }

    @GetMapping("/api/points/{pointProviderId}/usage-places")
    public ApiResponse<PointUsagePlaceListResponse> getPointUsagePlaces(
            @PathVariable("pointProviderId") Long pointProviderId,
            @RequestParam(value = "categoryId", required = false) Long categoryId
    ) {
        PointUsagePlaceListResponse data =
                pointService.getPointUsagePlaces(pointProviderId, categoryId);

        return ApiResponse.success("포인트 사용처 조회에 성공했습니다.", data);
    }
}
