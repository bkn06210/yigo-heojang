package com.wallet.engine.dao.dto;

/**
 * 혜택 하나의 기간 합산 소진 — user_benefit_usage의 여러 달 행을 SUM한 결과.
 *
 * 분기·연 한도 판정에 쓴다. 소진을 기간별 컬럼으로 저장하지 않고 월 행에서 합산하는 이유는,
 * 저장하면 기간이 바뀔 때 리셋 판정이 또 필요해지고 그 판정을 빠뜨리면 몇 달 뒤에야
 * 드러나는 형태로 조용히 틀리기 때문이다. 합산은 기간이 바뀌면 범위가 달라져 자연히 0부터 시작한다.
 *
 * 회원의 보유카드를 한 번에 조회하므로 그룹핑 키로 userCardId를 함께 가져온다.
 */
public class BenefitPeriodUsageRow {

    private long userCardId;
    private long benefitId;
    private long usedAmount;
    private int usedCount;

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
}
