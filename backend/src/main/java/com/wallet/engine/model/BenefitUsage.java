package com.wallet.engine.model;

/**
 * 혜택 하나의 소진 상태 — 기간 축마다 따로 든다.
 *
 * 일 소진(dailyUsed*)은 last_applied_date가 오늘일 때만 유효한 값이다.
 * 오늘이 아니면 0을 넣는다 — 날짜 리셋 판정은 엔진 밖(DAO·조립)에서 한다.
 *
 * 분기·연 소진(quarterlyUsed*·yearlyUsed*)은 그 기간에 속한 월 소진 행을 합산한 값이다.
 * 별도 컬럼으로 저장하지 않는 이유는, 저장하면 기간이 바뀔 때 리셋 판정이 또 필요해지고
 * 그 판정을 빠뜨리면 몇 달 뒤에야 드러나는 형태로 조용히 틀리기 때문이다.
 * 합산은 기간이 바뀌면 대상 범위가 달라져 자연히 0부터 시작한다.
 *
 * 횟수(monthlyUsedCount 등)는 "혜택을 받은 횟수"다. 단 COUNT_STEP(스탬프형) 혜택에서는
 * "스탬프가 찍힌 횟수"이며, 지급 횟수는 계산기가 step_count로 나눠 도출한다.
 */
public record BenefitUsage(
        long benefitId,
        long monthlyUsedAmount,
        int monthlyUsedCount,
        long dailyUsedAmount,
        int dailyUsedCount,
        long quarterlyUsedAmount,
        int quarterlyUsedCount,
        long yearlyUsedAmount,
        int yearlyUsedCount
) {

    /** 아직 한 번도 안 쓴 혜택 (user_benefit_usage에 행이 없는 경우) */
    public static BenefitUsage empty(long benefitId) {
        return new BenefitUsage(benefitId, 0L, 0, 0L, 0, 0L, 0, 0L, 0);
    }

    /** 월 소진만 있는 상태 — 분기·연 한도가 없는 혜택이면 합산 조회가 불필요하다 */
    public static BenefitUsage ofMonthly(long benefitId, long monthlyUsedAmount, int monthlyUsedCount,
                                         long dailyUsedAmount, int dailyUsedCount) {
        return new BenefitUsage(benefitId, monthlyUsedAmount, monthlyUsedCount,
                dailyUsedAmount, dailyUsedCount, 0L, 0, 0L, 0);
    }
}
