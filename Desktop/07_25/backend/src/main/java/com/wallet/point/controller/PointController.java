package com.wallet.point.controller;

import com.wallet.point.dto.PointHistoryListResponse;
import com.wallet.point.dto.PointListResponse;
import com.wallet.point.dto.PointUsagePlaceListResponse;
import com.wallet.point.service.PointService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class PointController {

    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    @GetMapping("/api/points")
    public Map<String, Object> getPointList() {

        // TODO: 로그인 붙으면 Access Token에서 memberId 꺼내기
        Long memberId = 1L;

        PointListResponse data = pointService.getPointList(memberId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "포인트 목록 조회에 성공했습니다.");
        response.put("data", data);

        return response;
    }

    @GetMapping("/api/points/history")
    public Map<String, Object> getPointHistory(
            @RequestParam(value = "pointWalletId", required = false) Long pointWalletId,
            @RequestParam(value = "pointType", required = false) String pointType,
            @RequestParam(value = "yearMonth", required = false) String yearMonth
    ) {
        // TODO: 로그인 붙으면 Access Token에서 memberId 꺼내기
        Long memberId = 1L;

        PointHistoryListResponse data =
                pointService.getPointHistory(memberId, pointWalletId, pointType, yearMonth);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "포인트 내역 조회에 성공했습니다.");
        response.put("data", data);

        return response;
    }

    @GetMapping("/api/points/{pointProviderId}/usage-places")
    public Map<String, Object> getPointUsagePlaces(
            @PathVariable("pointProviderId") Long pointProviderId,
            @RequestParam(value = "categoryId", required = false) Long categoryId
    ) {
        PointUsagePlaceListResponse data =
                pointService.getPointUsagePlaces(pointProviderId, categoryId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "포인트 사용처 조회에 성공했습니다.");
        response.put("data", data);

        return response;
    }
}