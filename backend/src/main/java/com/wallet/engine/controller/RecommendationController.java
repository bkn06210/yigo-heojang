package com.wallet.engine.controller;

import com.wallet.common.ApiResponse;
import com.wallet.engine.dto.RecommendationRequest;
import com.wallet.engine.dto.RecommendationResponse;
import com.wallet.engine.service.RecommendationService;
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
 * 결제 직전 카드 추천 API.
 *
 * 계산은 전부 서비스가 한다. 이 클래스는 요청을 받아 넘기고 응답 봉투를 씌우는 얇은 층이다.
 *
 * 회원 id는 요청 본문이 아니라 <b>인증 필터가 검증해 넣어 둔 값</b>에서 꺼낸다
 * (JwtAuthenticationFilter). 본문으로 받으면 남의 id를 적어 보내는 것을 막을 수 없다.
 *
 * 기준일(LocalDate.now())도 여기서 정해 서비스에 넘긴다 — 서비스와 계산기를 시계에서
 * 떼어 두면 "특정 날짜 기준" 테스트를 날짜 조작 없이 할 수 있다.
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * 가맹점(또는 카테고리)과 예상 결제금액을 받아 보유카드별 예상 혜택을 계산해 순위를 매긴다.
     *
     * 보유카드가 없어도 에러가 아니라 빈 목록을 반환한다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RecommendationResponse>> recommend(
            @RequestAttribute(AUTHENTICATED_MEMBER_ID) Long memberId,
            @Valid @RequestBody RecommendationRequest request) {

        RecommendationResponse response =
                recommendationService.recommend(memberId, request, LocalDate.now());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
