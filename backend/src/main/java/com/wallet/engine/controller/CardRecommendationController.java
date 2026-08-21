package com.wallet.engine.controller;

import com.wallet.common.ApiResponse;
import com.wallet.engine.dto.CardRecommendationResponse;
import com.wallet.engine.service.CardRecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static com.wallet.common.constant.RequestAttributeNames.AUTHENTICATED_MEMBER_ID;

/**
 * 소비 기반 카드 추천 — 지금 카드에 어떤 카드를 더하면 얼마나 좋아지는가.
 *
 * 결제 직전 추천(/api/recommendations)과 다른 API인 이유는 묻는 것이 다르기 때문이다.
 * 저쪽은 "이번 결제에 어느 카드를 쓸까"라 보유 카드만 보고 한 건을 계산한다.
 * 이쪽은 "어떤 카드를 발급할까"라 미보유 카드까지 놓고 한 달치를 계산한다.
 *
 * 입력이 없는 것은 근거가 그 회원의 지난달 소비 내역이기 때문이다.
 */
@RestController
@RequestMapping("/api/cards/recommendations")
public class CardRecommendationController {

    private final CardRecommendationService cardRecommendationService;

    public CardRecommendationController(CardRecommendationService cardRecommendationService) {
        this.cardRecommendationService = cardRecommendationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CardRecommendationResponse>> recommend(
            @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId) {

        CardRecommendationResponse response =
                cardRecommendationService.recommend(memberId, LocalDate.now());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
