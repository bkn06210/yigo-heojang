package com.wallet.engine.dto;

import java.util.Collections;
import java.util.List;

/**
 * 소비 기반 카드 추천 응답.
 *
 * <b>기준월과 현재 혜택액을 함께 내리는 이유</b> — 추천 금액이 "무엇과 비교한 차액"인지
 * 밝히지 않으면 화면이 그 숫자를 절대값처럼 쓰게 된다. 이 응답의 monthlyGainAmount는 전부
 * baseYearMonth 소비를 근거로 한 <b>currentMonthlyBenefitAmount 대비 증가분</b>이다.
 *
 * 소비 내역이 없으면 items가 비고 currentMonthlyBenefitAmount가 0이다. 에러가 아니다 —
 * 근거로 삼을 소비가 없을 뿐이라 화면은 "이번 달 소비가 쌓이면 추천해 드릴게요"로 안내하면 된다.
 *
 * @param baseYearMonth               추천 근거가 된 소비의 기준월 (YYYY-MM, 직전월)
 * @param currentMonthlyBenefitAmount 지금 보유 카드들로 그달에 받았을 총 혜택액
 * @param items                       추천 카드. 순증이 큰 순서. 순증이 0인 카드는 담지 않는다
 */
public record CardRecommendationResponse(
        String baseYearMonth,
        long currentMonthlyBenefitAmount,
        List<CardRecommendation> items
) {

    public CardRecommendationResponse {
        items = items == null ? List.of() : Collections.unmodifiableList(List.copyOf(items));
    }

    public static CardRecommendationResponse of(String baseYearMonth,
                                                long currentMonthlyBenefitAmount,
                                                List<CardRecommendation> items) {
        return new CardRecommendationResponse(baseYearMonth, currentMonthlyBenefitAmount, items);
    }
}
