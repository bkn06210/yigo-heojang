package com.wallet.engine.dto;

import java.util.List;

/**
 * 보유 카드 한 장이 이 결제 대상에서 갖는 혜택.
 *
 * 혜택이 하나도 없어도 목록에서 빼지 않는다. "이 카드는 여기서 혜택이 없다"도
 * 사용자가 알아야 하는 답이다.
 */
public class ApplicableBenefitCard {

    private long userCardId;
    private String cardName;

    /** 전월실적(원). 실적 조건이 걸린 혜택의 판정 근거다. */
    private long prevPerformanceAmount;

    /**
     * 실적 조건이 걸린 혜택을 받기 위해 필요한 전월실적(원).
     * 실적 구간이 0원 하나뿐인 카드는 null — 채울 실적이 없다는 뜻이다.
     */
    private Long requiredPerformanceAmount;

    /** 전월실적 조건 충족 여부. 판정된 구간의 최소실적이 0원보다 크면 충족이다. */
    private boolean performanceMet;

    private List<ApplicableBenefitItem> benefits;

    public ApplicableBenefitCard(long userCardId, String cardName, long prevPerformanceAmount,
                                 Long requiredPerformanceAmount, boolean performanceMet,
                                 List<ApplicableBenefitItem> benefits) {
        this.userCardId = userCardId;
        this.cardName = cardName;
        this.prevPerformanceAmount = prevPerformanceAmount;
        this.requiredPerformanceAmount = requiredPerformanceAmount;
        this.performanceMet = performanceMet;
        this.benefits = benefits;
    }

    public long getUserCardId() {
        return userCardId;
    }

    public String getCardName() {
        return cardName;
    }

    public long getPrevPerformanceAmount() {
        return prevPerformanceAmount;
    }

    public Long getRequiredPerformanceAmount() {
        return requiredPerformanceAmount;
    }

    public boolean isPerformanceMet() {
        return performanceMet;
    }

    public List<ApplicableBenefitItem> getBenefits() {
        return benefits;
    }
}
