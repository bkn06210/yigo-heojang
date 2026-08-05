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
import javax.servlet.http.HttpServletRequest;
import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

@RestController
public class PointController {

    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    @GetMapping("/api/points")
    public Map<String, Object> getPointList(HttpServletRequest request) {

        // TODO: 濡쒓렇??遺숈쑝硫?Access Token?먯꽌 memberId 爰쇰궡湲?
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        PointListResponse data = pointService.getPointList(memberId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "?ъ씤??紐⑸줉 議고쉶???깃났?덉뒿?덈떎.");
        response.put("data", data);

        return response;
    }

    @GetMapping("/api/points/history")
    public Map<String, Object> getPointHistory(
            @RequestParam(value = "pointWalletId", required = false) Long pointWalletId,
            @RequestParam(value = "pointType", required = false) String pointType,
            HttpServletRequest request,
            @RequestParam(value = "yearMonth", required = false) String yearMonth
    ) {
        // TODO: 濡쒓렇??遺숈쑝硫?Access Token?먯꽌 memberId 爰쇰궡湲?
        Long memberId = (Long) request.getAttribute(AUTHENTICATED_MEMBER_ID);

        PointHistoryListResponse data =
                pointService.getPointHistory(memberId, pointWalletId, pointType, yearMonth);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("code", "SUCCESS");
        response.put("message", "?ъ씤???댁뿭 議고쉶???깃났?덉뒿?덈떎.");
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
        response.put("message", "?ъ씤???ъ슜泥?議고쉶???깃났?덉뒿?덈떎.");
        response.put("data", data);

        return response;
    }
}
