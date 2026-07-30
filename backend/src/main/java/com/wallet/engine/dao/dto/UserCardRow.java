package com.wallet.engine.dao.dto;

/**
 * user_card 한 행의 조회 투영 — 보유카드 ID와 그 카드 마스터 ID·카드명.
 *
 * 엔진의 상태·거래 테이블은 user_card_id 기준이고, 실적구간·제외 규칙은 card_id 기준이다.
 * 서비스가 이 둘을 잇기 위해 보유카드에서 두 ID를 함께 가져온다.
 *
 * cardName은 card 마스터를 조인해 가져온다 — 추천·현황 응답이 카드명을 내려주는데,
 * 이걸 안 담으면 카드 수만큼 이름 조회가 따로 나간다. 복사 저장이 아니라 조회 시 조인이므로
 * 갱신 이상은 생기지 않는다.
 */
public class UserCardRow {

    private long userCardId;
    private long cardId;
    private String cardName;

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

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }
}
