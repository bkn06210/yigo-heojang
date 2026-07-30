package com.wallet.engine.service;

import com.wallet.engine.calculator.PerformanceTierResolver;
import com.wallet.engine.dao.dto.BenefitRow;
import com.wallet.engine.dto.BenefitUsageStatus;
import com.wallet.engine.dto.CardMonthlyStatus;
import com.wallet.engine.model.PerformanceProgress;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.PerformanceTier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 카드 월 상태 → 현황 응답(CardMonthlyStatus) 조립기.
 *
 * 취소 정산 응답과 개별 카드 상세 조회(#3)가 <b>같은 형식</b>을 쓰므로, 조립을 한 곳에 모아
 * 두 경로가 어긋나지 않게 한다(예: 한쪽은 목표금액을 전월실적 기준, 다른 쪽은 당월누적 기준으로
 * 잡는 자기모순 방지). 저장 숫자·구간·혜택 목록만 받아 파생값을 계산하는 순수 조립이라 DB·시계를 모른다.
 *
 * 세 축을 함께 얹는다:
 *   · 전월실적 축(judge)   → performanceMet, sharedLimit (현재 구간의 통합한도)
 *   · 당월누적 축(progressOf) → targetPerformance, remainingPerformance, achievementRate
 *   · 혜택별 이용 현황       → benefits[] (묶음 한도는 그룹 합산)
 */
@Component
public class CardMonthlyStatusBuilder {

    private final PerformanceTierResolver tierResolver = new PerformanceTierResolver();

    /**
     * 현황 응답을 조립한다.
     *
     * @param userCardId               보유카드 ID
     * @param cardName                 카드명
     * @param yearMonth                기준 연월 (YYYY-MM)
     * @param prevPerformanceAmount    전월 실적 (구간·통합한도 판정 기준)
     * @param currentPerformanceAmount 당월 누적 실적인정액 (진행률 기준)
     * @param sharedLimitUsed          통합한도 소진액
     * @param tiers                    카드의 실적구간 목록 (0원 구간 포함, 최소 1행)
     * @param benefitRows              카드의 활성 혜택(판정 구간의 개별한도 조인됨, GIFT·RETROACTIVE 제외)
     * @param usedAmountByBenefit      혜택 id → 이번 달 누적 혜택액(user_benefit_usage.used_amount)
     */
    public CardMonthlyStatus build(long userCardId, String cardName, String yearMonth,
                                   long prevPerformanceAmount, long currentPerformanceAmount,
                                   long sharedLimitUsed, List<PerformanceTier> tiers,
                                   List<BenefitRow> benefitRows, Map<Long, Long> usedAmountByBenefit) {
        // 전월실적 축 — 현재 적용 구간과 통합한도, 실적 충족 여부
        PerformanceStatus status = tierResolver.judge(tiers, prevPerformanceAmount);
        // 당월누적 축 — 다음 목표까지의 진행률
        PerformanceProgress progress = tierResolver.progressOf(tiers, currentPerformanceAmount);

        return new CardMonthlyStatus(
                userCardId,
                cardName,
                yearMonth,
                prevPerformanceAmount,
                progress.targetPerformance(),
                currentPerformanceAmount,
                progress.remainingPerformance(),
                progress.achievementRate(),
                status.performanceMet(),
                status.sharedMonthlyLimit(),
                sharedLimitUsed,
                buildBenefits(benefitRows, usedAmountByBenefit));
    }

    /**
     * 혜택별 이용 현황을 조립한다. <b>한도가 있는 혜택만</b> 담는다 —
     * 한도 없는 혜택(예: 전 가맹점 기본 적립)은 잔여·이용률이 정의되지 않아 이용 현황에서 뺀다.
     *
     * 묶음 한도(limit_group_code) 소속 혜택은 usedAmount를 <b>그룹 합산</b>으로 내려준다.
     * 화면이 혜택마다 따로 더하면 한도가 그룹 혜택 수만큼 배로 보이므로, 묶인 혜택들은 같은 값을 갖는다.
     */
    private List<BenefitUsageStatus> buildBenefits(List<BenefitRow> benefitRows,
                                                   Map<Long, Long> usedAmountByBenefit) {
        List<BenefitUsageStatus> result = new ArrayList<>();
        for (BenefitRow row : benefitRows) {
            Long monthlyLimit = effectiveMonthlyLimit(row);
            if (monthlyLimit == null) {
                continue;
            }
            long usedAmount = groupUsedAmount(row, benefitRows, usedAmountByBenefit);
            long remainingLimit = Math.max(0L, monthlyLimit - usedAmount);
            // 한도 0(혜택 없음)이면 이용률은 정의되지 않는다 — 0으로 나누지 않도록 null
            BigDecimal usageRate = (monthlyLimit == 0)
                    ? null
                    : BigDecimal.valueOf(usedAmount * 100)
                            .divide(BigDecimal.valueOf(monthlyLimit), 1, RoundingMode.HALF_UP);
            result.add(new BenefitUsageStatus(
                    row.getBenefitId(), row.getBenefitName(), row.getLimitGroupCode(),
                    usedAmount, monthlyLimit, remainingLimit, usageRate));
        }
        return result;
    }

    /** 판정된 구간의 개별한도가 있으면 그 값, 없으면 base 월 한도. 둘 다 없으면 null(한도 없음). */
    private Long effectiveMonthlyLimit(BenefitRow row) {
        return row.getTierMonthlyLimit() != null ? row.getTierMonthlyLimit() : row.getMonthlyLimit();
    }

    /**
     * 월 한도 표시에 쓸 소진액. 묶음이면 같은 코드를 가진 혜택들의 소진액 합이다.
     * (CardBenefitSelector의 계산용 그룹 합산과 같은 규칙 — 표시와 계산이 어긋나지 않게 맞춘다.)
     */
    private long groupUsedAmount(BenefitRow row, List<BenefitRow> allRows,
                                 Map<Long, Long> usedAmountByBenefit) {
        String groupCode = row.getLimitGroupCode();
        if (groupCode == null) {
            return usedAmountByBenefit.getOrDefault(row.getBenefitId(), 0L);
        }
        return allRows.stream()
                .filter(other -> groupCode.equals(other.getLimitGroupCode()))
                .mapToLong(other -> usedAmountByBenefit.getOrDefault(other.getBenefitId(), 0L))
                .sum();
    }
}
