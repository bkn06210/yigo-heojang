package com.wallet.engine.controller;

import com.wallet.common.ApiResponse;
import com.wallet.engine.dto.CardMonthlyStatus;
import com.wallet.engine.dto.SettlementCancelRequest;
import com.wallet.engine.service.SettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

/**
 * 결제 취소 정산 API.
 *
 * 결제(가산)는 소비내역 도메인이 같은 트랜잭션에서 엔진을 자바 메서드로 직접 호출하므로 REST가 아니다.
 * 취소(차감)만 외부(카드사/마이데이터)에서 유입되는 사실이라 REST 엔드포인트를 둔다.
 *
 * 회원 id는 요청 본문이 아니라 인증 필터가 넣어 둔 값에서 꺼낸다 — 남의 소비내역 취소를 막는다.
 * 기준일(LocalDate.now())도 여기서 정해 서비스에 넘겨, 서비스·계산기를 시계에서 떼어 둔다.
 */
@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    /**
     * 취소된 소비내역의 기여분을 상태에서 역산 차감하고, 차감 반영된 카드 현황을 반환한다.
     */
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<CardMonthlyStatus>> cancel(
            @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId,
            @Valid @RequestBody SettlementCancelRequest request) {

        CardMonthlyStatus status =
                settlementService.cancelPayment(memberId, request.getExpenseId(), LocalDate.now());

        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
