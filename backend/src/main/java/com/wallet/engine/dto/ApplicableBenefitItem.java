package com.wallet.engine.dto;

import java.math.BigDecimal;

/**
 * 이 결제 대상에 걸리는 혜택 하나와 그 조건.
 *
 * 결제 직전 추천이 "얼마 받는다"를 내려주는 것과 달리 여기는 "어떤 조건으로 받는다"를 담는다.
 * 결제 금액이 정해지지 않은 질문("이 가맹점에서 어느 카드가 좋아?")에 답하려면 금액이 아니라
 * 조건이 필요하기 때문이다.
 *
 * 못 받는 혜택도 빼지 않고 사유와 함께 담는다. "실적을 채우면 받을 수 있다"는 정보가
 * "혜택이 없다"보다 쓸모 있다.
 */
public class ApplicableBenefitItem {

    private long benefitId;
    private String benefitName;
    private String benefitKind;
    private String calcMethod;

    /** RATE면 퍼센트, FIXED·COUNT_STEP이면 원. 판정된 실적구간의 값이 반영된 금액이다. */
    private BigDecimal benefitValue;

    /** 전월(또는 전분기) 실적 조건이 걸린 혜택인지. */
    private boolean requirePerformance;

    /** 지금 이 카드로 받을 수 있는지. */
    private boolean available;

    /** available이 false인 이유. 받을 수 있으면 null. */
    private String unavailableReason;

    /** 건당 최소 결제금액. null = 제약 없음. */
    private Long minTxnAmount;

    /** 월 한도. null = 제약 없음, 0 = 혜택 없음. 둘은 뜻이 다르다. */
    private Long monthlyLimit;

    /** N회마다 지급하는 혜택의 N. 아니면 null. */
    private Integer stepCount;

    /** 매월 택1하는 혜택의 묶음 코드. null이면 선택과 무관하게 적용된다. */
    private String optionGroupCode;

    public ApplicableBenefitItem(long benefitId, String benefitName, String benefitKind,
                                 String calcMethod, BigDecimal benefitValue, boolean requirePerformance,
                                 boolean available, String unavailableReason, Long minTxnAmount,
                                 Long monthlyLimit, Integer stepCount, String optionGroupCode) {
        this.benefitId = benefitId;
        this.benefitName = benefitName;
        this.benefitKind = benefitKind;
        this.calcMethod = calcMethod;
        this.benefitValue = benefitValue;
        this.requirePerformance = requirePerformance;
        this.available = available;
        this.unavailableReason = unavailableReason;
        this.minTxnAmount = minTxnAmount;
        this.monthlyLimit = monthlyLimit;
        this.stepCount = stepCount;
        this.optionGroupCode = optionGroupCode;
    }

    public long getBenefitId() {
        return benefitId;
    }

    public String getBenefitName() {
        return benefitName;
    }

    public String getBenefitKind() {
        return benefitKind;
    }

    public String getCalcMethod() {
        return calcMethod;
    }

    public BigDecimal getBenefitValue() {
        return benefitValue;
    }

    public boolean isRequirePerformance() {
        return requirePerformance;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getUnavailableReason() {
        return unavailableReason;
    }

    public Long getMinTxnAmount() {
        return minTxnAmount;
    }

    public Long getMonthlyLimit() {
        return monthlyLimit;
    }

    public Integer getStepCount() {
        return stepCount;
    }

    public String getOptionGroupCode() {
        return optionGroupCode;
    }
}
