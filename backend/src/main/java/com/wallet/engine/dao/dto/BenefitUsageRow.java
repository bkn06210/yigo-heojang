package com.wallet.engine.dao.dto;

import java.time.LocalDate;

/**
 * user_benefit_usage 한 행의 조회 투영 — 혜택 하나의 이번 달 소진 현황.
 *
 * 회원의 보유카드를 한 번에 조회하므로 그룹핑 키로 userCardId를 함께 가져온다.
 *
 * 일 소진(dailyUsedAmount/Count)은 <b>lastAppliedDate 당일의 값</b>이라 원값 그대로는 쓸 수 없다.
 * 어제 값이 남아 있으면 오늘 판정에 0으로 봐야 하며, 그 리셋 판정은 CardStateAssembler가 한다
 * (DAO는 원값만 반환한다는 기존 계약 유지).
 */
public class BenefitUsageRow {

    private long userCardId;
    private long benefitId;
    private long usedAmount;
    private int usedCount;
    private LocalDate lastAppliedDate;
    private long dailyUsedAmount;
    private int dailyUsedCount;

    public long getUserCardId() {
        return userCardId;
    }

    public void setUserCardId(long userCardId) {
        this.userCardId = userCardId;
    }

    public long getBenefitId() {
        return benefitId;
    }

    public void setBenefitId(long benefitId) {
        this.benefitId = benefitId;
    }

    public long getUsedAmount() {
        return usedAmount;
    }

    public void setUsedAmount(long usedAmount) {
        this.usedAmount = usedAmount;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public LocalDate getLastAppliedDate() {
        return lastAppliedDate;
    }

    public void setLastAppliedDate(LocalDate lastAppliedDate) {
        this.lastAppliedDate = lastAppliedDate;
    }

    public long getDailyUsedAmount() {
        return dailyUsedAmount;
    }

    public void setDailyUsedAmount(long dailyUsedAmount) {
        this.dailyUsedAmount = dailyUsedAmount;
    }

    public int getDailyUsedCount() {
        return dailyUsedCount;
    }

    public void setDailyUsedCount(int dailyUsedCount) {
        this.dailyUsedCount = dailyUsedCount;
    }
}
