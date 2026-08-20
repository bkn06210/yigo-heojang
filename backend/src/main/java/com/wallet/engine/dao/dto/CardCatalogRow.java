package com.wallet.engine.dao.dto;

/** 카드 마스터 한 줄 — 추천 결과 표시용 이름·카드사·최저 연회비 */
public class CardCatalogRow {

    private long cardId;
    private String cardName;
    private String cardCompanyName;
    private long minAnnualFee;

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

    public String getCardCompanyName() {
        return cardCompanyName;
    }

    public void setCardCompanyName(String cardCompanyName) {
        this.cardCompanyName = cardCompanyName;
    }

    public long getMinAnnualFee() {
        return minAnnualFee;
    }

    public void setMinAnnualFee(long minAnnualFee) {
        this.minAnnualFee = minAnnualFee;
    }
}
