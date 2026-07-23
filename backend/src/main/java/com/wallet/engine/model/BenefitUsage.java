package com.wallet.engine.model;

/**
 * 혜택 하나의 이번 달 소진 상태 (user_benefit_usage 한 행).
 *
 * 일 소진(dailyUsedAmount/Count)은 last_applied_date가 오늘일 때만 유효한 값이다.
 * 오늘이 아니면 0을 넣는다 — 날짜 리셋 판정은 엔진 밖(DAO)에서 한다.
 */
public record BenefitUsage(
        long benefitId,
        long monthlyUsedAmount,
        int monthlyUsedCount,
        long dailyUsedAmount,
        int dailyUsedCount
) {

    /** 아직 한 번도 안 쓴 혜택 (user_benefit_usage에 행이 없는 경우) */
    public static BenefitUsage empty(long benefitId) {
        return new BenefitUsage(benefitId, 0L, 0, 0L, 0);
    }
}
