package com.wallet.engine.dto;

import java.util.List;

/**
 * 결제 직전 추천 응답의 data 부분.
 *
 * 미구현 필드({@link #futureOptimization}, {@link #pointGuide}, {@link #membershipEarn})도
 * <b>자리를 비워 두지 않고 null·빈 배열로 내려보낸다.</b> 프론트 입장에서 "필드가 없음"과
 * "값이 없음"은 다르게 동작하므로, 명세에 있는 형태를 지금부터 지켜 둔다.
 *
 * @param recommendations    보유카드별 추천 결과. 혜택이 없는 카드도 포함하며 순위 오름차순이다
 * @param futureOptimization 실적 미달 경고. 현재 항상 null
 * @param pointGuide         금융포인트 잔액 안내. 현재 항상 null
 * @param membershipEarn     등록 멤버십 적립 안내. 현재 항상 빈 배열
 */
public record RecommendationResponse(
        List<RecommendationItem> recommendations,
        FutureOptimization futureOptimization,
        PointGuide pointGuide,
        List<MembershipEarn> membershipEarn
) {

    /**
     * 추천 목록만 채운 응답 — 나머지 세 필드는 미구현이라 비운다.
     * 각 필드가 채워지는 시점에 이 팩토리를 확장하거나 생성자를 직접 쓴다.
     */
    public static RecommendationResponse of(List<RecommendationItem> recommendations) {
        return new RecommendationResponse(recommendations, null, null, List.of());
    }
}
