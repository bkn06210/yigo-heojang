package com.wallet.engine.dto;

import java.util.Comparator;

/**
 * 추천 카드 한 장.
 *
 * @param monthlyGainAmount 이 카드를 더 썼을 때 한 달에 늘어나는 혜택액. 지금 카드들로 받는
 *                          금액과의 <b>차액</b>이라 절대 음수가 아니다
 * @param annualFee         최저 연회비. 브랜드·발급형태에 따라 더 비쌀 수 있어 하한으로 읽어야 한다
 * @param breakEvenMonths   연회비를 넘어서는 데 걸리는 개월 수. 연회비가 없으면 0
 */
public record CardRecommendation(
        long cardId,
        String cardName,
        String cardCompanyName,
        long monthlyGainAmount,
        long annualFee,
        int breakEvenMonths
) {

    /** 순증이 큰 순서. 같으면 연회비가 싼 쪽, 그것도 같으면 cardId 오름차순(재현성) */
    public static final Comparator<CardRecommendation> BY_GAIN_DESC =
            Comparator.comparingLong(CardRecommendation::monthlyGainAmount).reversed()
                    .thenComparingLong(CardRecommendation::annualFee)
                    .thenComparingLong(CardRecommendation::cardId);

    public static CardRecommendation of(long cardId, String cardName, String cardCompanyName,
                                        long monthlyGainAmount, long annualFee, int breakEvenMonths) {
        return new CardRecommendation(cardId, cardName, cardCompanyName,
                monthlyGainAmount, annualFee, breakEvenMonths);
    }
}
