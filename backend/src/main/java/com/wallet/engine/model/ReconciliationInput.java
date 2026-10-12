package com.wallet.engine.model;

import java.util.List;

/**
 * 대사의 입력 — 저장된 상태와, 같은 기간의 소비내역에서 다시 구한 값.
 *
 * 비교기가 DB를 모르게 하려고 양쪽을 이미 조회·집계된 모양으로 받는다.
 * "저장된 것(Stored)"과 "원본에서 구한 것(Source)"을 타입으로 갈라, 둘을 뒤바꿔 넣는 실수를 막는다.
 *
 * @param fromYearMonth       대조 시작 연월 (YYYY-MM). 이보다 앞선 카드 월 상태는 대조 대상이 아니라
 *                            전월실적 이월을 맞춰 볼 기준으로만 쓴다
 * @param storedCardMonths    저장된 카드 월 상태. 이월 대조를 위해 시작 연월의 직전 월 행까지 담는다
 * @param storedBenefitUsages 저장된 혜택 소진
 * @param sourceCardMonths    소비내역이 있는 (보유카드, 월)마다 다시 구한 실적인정액
 * @param sourceBenefitUsages 혜택이 적용된 소비내역을 (보유카드, 혜택, 월)로 묶은 합계
 */
public record ReconciliationInput(String fromYearMonth,
                                  List<StoredCardMonth> storedCardMonths,
                                  List<StoredBenefitUsage> storedBenefitUsages,
                                  List<SourceCardMonth> sourceCardMonths,
                                  List<SourceBenefitUsage> sourceBenefitUsages) {

    public ReconciliationInput {
        if (fromYearMonth == null || fromYearMonth.isBlank()) {
            throw new IllegalArgumentException("대조 시작 연월은 필수다");
        }
        storedCardMonths = List.copyOf(storedCardMonths);
        storedBenefitUsages = List.copyOf(storedBenefitUsages);
        sourceCardMonths = List.copyOf(sourceCardMonths);
        sourceBenefitUsages = List.copyOf(sourceBenefitUsages);
    }

    /** user_card_monthly_state 한 행 */
    public record StoredCardMonth(long userCardId,
                                  String baseYearMonth,
                                  long prevPerformanceAmount,
                                  long currentPerformanceAmount,
                                  long sharedLimitUsed) {
    }

    /**
     * user_benefit_usage 한 행.
     *
     * @param stampBenefit COUNT_STEP(스탬프) 혜택인지. 스탬프의 횟수는 원본에 흔적이 남지 않는다
     */
    public record StoredBenefitUsage(long userCardId,
                                     long benefitId,
                                     String baseYearMonth,
                                     long usedAmount,
                                     long usedCount,
                                     boolean stampBenefit) {
    }

    /**
     * 한 보유카드의 한 달 소비내역에서 다시 구한 값.
     *
     * 그 달에 소비내역이 한 건이라도 있으면(취소 건 포함) 만들어진다 — 전부 취소됐거나 전부
     * 실적 제외여도 "원본은 있고 실적인정액이 0"이지 "원본이 없다"가 아니다.
     *
     * @param expenseCount      그 달 소비내역 건수(취소 건 포함)
     * @param performanceAmount 실적 규칙으로 다시 합산한 실적인정액
     */
    public record SourceCardMonth(long userCardId,
                                  String baseYearMonth,
                                  int expenseCount,
                                  long performanceAmount) {
    }

    /**
     * 한 혜택이 적용된 소비내역의 월 합계.
     *
     * 그 혜택이 적용된 소비내역이 한 건이라도 있으면(취소 건 포함) 만들어진다. 합계와 건수는
     * 취소되지 않은 건만 센다 — 취소는 결제 때 더한 만큼을 상태에서 도로 빼기 때문이다.
     *
     * @param discountAmount  취소되지 않은 건의 discount_amount 합
     * @param appliedCount    취소되지 않은 건수
     * @param usesSharedLimit 이 혜택이 통합할인한도를 쓰는지. 쓰면 합계가 통합한도 사용액에도 들어간다
     * @param stampBenefit    COUNT_STEP(스탬프) 혜택인지
     */
    public record SourceBenefitUsage(long userCardId,
                                     long benefitId,
                                     String baseYearMonth,
                                     long discountAmount,
                                     long appliedCount,
                                     boolean usesSharedLimit,
                                     boolean stampBenefit) {
    }
}
