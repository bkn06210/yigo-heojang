package com.wallet.engine.dao.dto;

/**
 * 혜택 적용 예외 한 행의 조회 투영 — benefit_exclusion 또는 card_benefit_exclusion.
 *
 * 두 표의 행이 한 목록으로 온다. benefitId가 있으면 그 혜택 하나에 걸리는 예외이고,
 * null이면 그 카드의 모든 혜택에 걸리는 카드 전체 예외다. 카드 전체 예외는 조회에서 혜택 수만큼
 * 펼치지 않고 저장된 행 그대로 오며, 혜택마다 붙이는 일은 BenefitCandidateAssembler가 한다.
 *
 * exclusion_value는 id가 아니라 코드 문자열이다(CATEGORY면 category_code, MERCHANT면 merchant_code).
 */
public class BenefitExclusionRow {

    // 카드 전체 예외를 어느 카드의 혜택에 붙일지 가른다
    private long cardId;
    // 카드 전체 예외는 특정 혜택에 속하지 않아 null이다
    private Long benefitId;
    private String exclusionType;
    private String exclusionValue;

    /** 그 카드의 모든 혜택에 걸리는 예외인가 */
    public boolean isCardWide() {
        return benefitId == null;
    }

    public Long getBenefitId() {
        return benefitId;
    }

    public void setBenefitId(Long benefitId) {
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
