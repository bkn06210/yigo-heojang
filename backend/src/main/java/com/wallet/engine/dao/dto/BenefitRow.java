package com.wallet.engine.dao.dto;

import java.math.BigDecimal;

/**
 * benefit 한 행 + 판정된 구간의 benefit_tier_limit 덧붙임(LEFT JOIN)의 조회 투영.
 *
 * 상한·조건 컬럼은 Long/Integer/BigDecimal이다 — NULL(제약 없음)과 0(혜택 없음)의 의미가
 * 다르므로 원시 타입으로 받으면 안 된다. 'Y'/'N'·enum 문자열의 해석은 여기서 하지 않고
 * BenefitCandidateAssembler가 한다 (DAO는 flat row만 반환한다).
 *
 * tier*는 구간별 개별한도(benefit_tier_limit) 조인 결과다.
 * 판정된 구간에 해당 행이 없으면 NULL이며, TierLimitResolver가 base 값을 그대로 유지한다.
 */
public class BenefitRow {

    private long benefitId;
    private String benefitName;
    private String benefitKind;
    private String calcMethod;
    private BigDecimal benefitValue;
    private String targetType;
    private Long targetCategoryId;
    private Long targetMerchantId;
    private String requirePerformance;
    private String requirePaymentType;
    private Long minTxnAmount;
    private Long maxEligibleAmount;
    private Long maxBenefitPerTxn;
    private Long monthlyLimit;
    private String limitGroupCode;
    private Integer monthlyCountLimit;
    private Integer dailyCountLimit;
    private Long dailyLimit;
    private Long quarterlyLimit;
    private Long yearlyLimit;
    private Integer quarterlyCountLimit;
    private Integer yearlyCountLimit;
    private String countGroupCode;
    private Integer stepCount;
    private String performancePeriod;
    private String excludeFromPerformance;
    private String optionGroupCode;
    private String optionKey;
    private String useSharedLimit;
    private Long tierMonthlyLimit;
    private Long tierQuarterlyLimit;
    private Long tierYearlyLimit;
    private BigDecimal tierBenefitValue;

    public long getBenefitId() {
        return benefitId;
    }

    public void setBenefitId(long benefitId) {
        this.benefitId = benefitId;
    }

    public String getBenefitName() {
        return benefitName;
    }

    public void setBenefitName(String benefitName) {
        this.benefitName = benefitName;
    }

    public String getBenefitKind() {
        return benefitKind;
    }

    public void setBenefitKind(String benefitKind) {
        this.benefitKind = benefitKind;
    }

    public String getCalcMethod() {
        return calcMethod;
    }

    public void setCalcMethod(String calcMethod) {
        this.calcMethod = calcMethod;
    }

    public BigDecimal getBenefitValue() {
        return benefitValue;
    }

    public void setBenefitValue(BigDecimal benefitValue) {
        this.benefitValue = benefitValue;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetCategoryId() {
        return targetCategoryId;
    }

    public void setTargetCategoryId(Long targetCategoryId) {
        this.targetCategoryId = targetCategoryId;
    }

    public Long getTargetMerchantId() {
        return targetMerchantId;
    }

    public void setTargetMerchantId(Long targetMerchantId) {
        this.targetMerchantId = targetMerchantId;
    }

    public String getRequirePerformance() {
        return requirePerformance;
    }

    public void setRequirePerformance(String requirePerformance) {
        this.requirePerformance = requirePerformance;
    }

    public String getRequirePaymentType() {
        return requirePaymentType;
    }

    public void setRequirePaymentType(String requirePaymentType) {
        this.requirePaymentType = requirePaymentType;
    }

    public Long getMinTxnAmount() {
        return minTxnAmount;
    }

    public void setMinTxnAmount(Long minTxnAmount) {
        this.minTxnAmount = minTxnAmount;
    }

    public Long getMaxEligibleAmount() {
        return maxEligibleAmount;
    }

    public void setMaxEligibleAmount(Long maxEligibleAmount) {
        this.maxEligibleAmount = maxEligibleAmount;
    }

    public Long getMaxBenefitPerTxn() {
        return maxBenefitPerTxn;
    }

    public void setMaxBenefitPerTxn(Long maxBenefitPerTxn) {
        this.maxBenefitPerTxn = maxBenefitPerTxn;
    }

    public Long getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(Long monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public String getLimitGroupCode() {
        return limitGroupCode;
    }

    public void setLimitGroupCode(String limitGroupCode) {
        this.limitGroupCode = limitGroupCode;
    }

    public Integer getMonthlyCountLimit() {
        return monthlyCountLimit;
    }

    public void setMonthlyCountLimit(Integer monthlyCountLimit) {
        this.monthlyCountLimit = monthlyCountLimit;
    }

    public Integer getDailyCountLimit() {
        return dailyCountLimit;
    }

    public void setDailyCountLimit(Integer dailyCountLimit) {
        this.dailyCountLimit = dailyCountLimit;
    }

    public Long getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Long dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Long getQuarterlyLimit() {
        return quarterlyLimit;
    }

    public void setQuarterlyLimit(Long quarterlyLimit) {
        this.quarterlyLimit = quarterlyLimit;
    }

    public Long getYearlyLimit() {
        return yearlyLimit;
    }

    public void setYearlyLimit(Long yearlyLimit) {
        this.yearlyLimit = yearlyLimit;
    }

    public Integer getQuarterlyCountLimit() {
        return quarterlyCountLimit;
    }

    public void setQuarterlyCountLimit(Integer quarterlyCountLimit) {
        this.quarterlyCountLimit = quarterlyCountLimit;
    }

    public Integer getYearlyCountLimit() {
        return yearlyCountLimit;
    }

    public void setYearlyCountLimit(Integer yearlyCountLimit) {
        this.yearlyCountLimit = yearlyCountLimit;
    }

    public String getCountGroupCode() {
        return countGroupCode;
    }

    public void setCountGroupCode(String countGroupCode) {
        this.countGroupCode = countGroupCode;
    }

    public Integer getStepCount() {
        return stepCount;
    }

    public void setStepCount(Integer stepCount) {
        this.stepCount = stepCount;
    }

    public String getPerformancePeriod() {
        return performancePeriod;
    }

    public void setPerformancePeriod(String performancePeriod) {
        this.performancePeriod = performancePeriod;
    }

    public String getExcludeFromPerformance() {
        return excludeFromPerformance;
    }

    public void setExcludeFromPerformance(String excludeFromPerformance) {
        this.excludeFromPerformance = excludeFromPerformance;
    }

    public String getOptionGroupCode() {
        return optionGroupCode;
    }

    public void setOptionGroupCode(String optionGroupCode) {
        this.optionGroupCode = optionGroupCode;
    }

    public String getOptionKey() {
        return optionKey;
    }

    public void setOptionKey(String optionKey) {
        this.optionKey = optionKey;
    }

    public String getUseSharedLimit() {
        return useSharedLimit;
    }

    public void setUseSharedLimit(String useSharedLimit) {
        this.useSharedLimit = useSharedLimit;
    }

    public Long getTierMonthlyLimit() {
        return tierMonthlyLimit;
    }

    public void setTierMonthlyLimit(Long tierMonthlyLimit) {
        this.tierMonthlyLimit = tierMonthlyLimit;
    }

    public Long getTierQuarterlyLimit() {
        return tierQuarterlyLimit;
    }

    public void setTierQuarterlyLimit(Long tierQuarterlyLimit) {
        this.tierQuarterlyLimit = tierQuarterlyLimit;
    }

    public Long getTierYearlyLimit() {
        return tierYearlyLimit;
    }

    public void setTierYearlyLimit(Long tierYearlyLimit) {
        this.tierYearlyLimit = tierYearlyLimit;
    }

    public BigDecimal getTierBenefitValue() {
        return tierBenefitValue;
    }

    public void setTierBenefitValue(BigDecimal tierBenefitValue) {
        this.tierBenefitValue = tierBenefitValue;
    }
}
