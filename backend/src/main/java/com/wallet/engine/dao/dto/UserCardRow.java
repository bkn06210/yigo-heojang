package com.wallet.engine.dao.dto;

/**
 * user_card 한 행의 최소 조회 투영 — 보유카드 ID와 그 카드 마스터 ID.
 *
 * 엔진의 상태·거래 테이블은 user_card_id 기준이고, 실적구간·제외 규칙은 card_id 기준이다.
 * 서비스가 이 둘을 잇기 위해 보유카드에서 두 ID를 함께 가져온다.
 */
public class UserCardRow {

    private long userCardId;
    private long cardId;

    public long getUserCardId() {
        return userCardId;
    }

    public void setUserCardId(long userCardId) {
        this.userCardId = userCardId;
    }

    public long getCardId() {
        return cardId;
    }

    public void setCardId(long cardId) {
        this.cardId = cardId;
    }
}
