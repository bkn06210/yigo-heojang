package com.wallet.engine.controller;

import com.wallet.common.ApiResponse;
import com.wallet.engine.dto.ApplicableBenefitResponse;
import com.wallet.engine.service.ApplicableBenefitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

/**
 * 특정 가맹점·업종에서 보유 카드가 갖는 혜택 조회.
 *
 * 결제 직전 추천과 답하는 질문이 다르다. 추천은 금액이 있어야 순위를 낼 수 있지만,
 * "이 가맹점 가면 어느 카드가 좋아?"는 금액 없이 나오는 질문이다. 그때 필요한 것은
 * 금액이 아니라 혜택의 조건(적립률·실적조건·건당 최소금액)이다.
 *
 * 회원 id는 요청 파라미터가 아니라 인증 필터가 검증해 넣어 둔 값에서 꺼낸다.
 */
@RestController
@RequestMapping("/api/cards/applicable-benefits")
public class ApplicableBenefitController {

    private final ApplicableBenefitService applicableBenefitService;

    public ApplicableBenefitController(ApplicableBenefitService applicableBenefitService) {
        this.applicableBenefitService = applicableBenefitService;
    }

    /**
     * @param merchantId 가맹점 ID. 주면 그 가맹점 혜택과 소속 업종 혜택을 함께 본다
     * @param categoryId 업종 ID. merchantId가 있으면 무시된다.
     *                   둘 다 없으면 전 가맹점(ALL) 혜택만 나온다
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ApplicableBenefitResponse>> lookup(
            @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) Long categoryId) {

        ApplicableBenefitResponse response =
                applicableBenefitService.lookup(memberId, merchantId, categoryId, LocalDate.now());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
