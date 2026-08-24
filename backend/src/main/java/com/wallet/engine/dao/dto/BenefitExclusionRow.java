package com.wallet.engine.dao.dto;

/**
 * benefit_exclusion 한 행의 조회 투영.
 *
 * exclusion_value는 id가 아니라 코드 문자열이다(CATEGORY면 category_code, MERCHANT면 merchant_code).
 * benefitId로 그룹핑해 각 BenefitCandidate에 붙인다.
 */
public class BenefitExclusionRow {

    // 카드 여러 장을 한 번에 조회할 때 어느 카드의 것인지 가른다
    private long cardId;
    private long benefitId;
    private String exclusionType;
    private String exclusionValue;

    public long getBenefitId() {
        return benefitId;
    }

    public void setBenefitId(long benefitId) {
        this.benefitId = benefitId;
    }

    public String getExclusionType() {
        return exclusionType;
    }

    public void setExclusionType(String exclusionType) {
        this.exclusionType = exclusionType;
    }

    public String getExclusionValue() {
        return exclusionValue;
    }

    public void setExclusionValue(String exclusionValue) {
        this.exclusionValue = exclusionValue;
    }

    public long getCardId() {
        return cardId;
    }

    public void setCardId(long cardId) {
        this.cardId = cardId;
    }
}
