package com.wallet.engine.dto;

import java.util.List;

/**
 * 특정 가맹점·업종에서 보유 카드가 갖는 혜택 목록.
 *
 * 보유 카드가 없어도 에러가 아니라 빈 목록이다.
 */
public class ApplicableBenefitResponse {

    private List<ApplicableBenefitCard> cards;

    public ApplicableBenefitResponse(List<ApplicableBenefitCard> cards) {
        this.cards = cards;
    }

    public List<ApplicableBenefitCard> getCards() {
        return cards;
    }
}
