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

    /** 이용률 상한. 소진이 한도를 넘는 데이터가 있어도 100%를 넘겨 표시하지 않는다 */
    private static final BigDecimal MAX_USAGE_RATE = new BigDecimal("100.0");

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
     * @param selectedOptionKeys       선택형 혜택 묶음의 그달 선택 (option_group_code → option_key)
     */
    public CardMonthlyStatus build(long userCardId, String cardName, String yearMonth,
                                   long prevPerformanceAmount, long currentPerformanceAmount,
                                   long sharedLimitUsed, List<PerformanceTier> tiers,
                                   List<BenefitRow> benefitRows, Map<Long, Long> usedAmountByBenefit,
                                   Map<String, String> selectedOptionKeys) {
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
                buildBenefits(benefitRows, usedAmountByBenefit, selectedOptionKeys));
    }

    /**
     * 혜택별 이용 현황을 조립한다. <b>한도 없는 혜택도 담는다</b> — 카드 상세 화면이 이 응답 하나로
     * "이 카드로 받을 수 있는 혜택"을 그릴 수 있어야 하므로, 목록에서 빼는 대신
     * 정의되지 않는 세 값(monthlyLimit·remainingLimit·usageRate)을 null로 내려준다.
     * NULL≠0 원칙과 같은 표현이다 — null은 "제약 없음"이지 "다 썼음"이 아니다.
     *
     * 묶음 한도(limit_group_code) 소속 혜택은 usedAmount를 <b>그룹 합산</b>으로 내려준다.
     * 화면이 혜택마다 따로 더하면 한도가 그룹 혜택 수만큼 배로 보이므로, 묶인 혜택들은 같은 값을 갖는다.
     *
     * <b>실적 조건으로 걸러내지는 않는다.</b> 카드 상세는 "이 카드에 어떤 혜택이 있나"를 보는 자리라
     * 지금 못 쓰는 혜택도 보여야 한다. 대신 {@code requirePerformance}를 함께 내려, 카드의
     * {@code performanceMet}과 묶어 보면 "지금 받을 수 있나"를 판단할 수 있게 한다.
     * 홈 요약(#2)이 "지금 쓸 수 있는 것"만 남기는 필터는 CardStatusOverviewBuilder가 수행한다.
     *
     * <b>선택형 혜택(매월 택1)은 고른 선택지만 담는다.</b> 실적 조건과 달리 여기서는 걸러낸다 —
     * 실적은 채우면 이번 달에 받을 수 있지만, 고르지 않은 선택지는 이번 달에 받을 방법이 없어
     * 화면에 두면 "받을 수 있는 혜택"을 잘못 알려주는 것이 된다. 계산기(CardBenefitSelector)가
     * 같은 규칙으로 후보에서 빼므로, 화면 목록과 실제 계산 대상이 어긋나지 않는다.
     */
    private List<BenefitUsageStatus> buildBenefits(List<BenefitRow> benefitRows,
                                                   Map<Long, Long> usedAmountByBenefit,
                                                   Map<String, String> selectedOptionKeys) {
        List<BenefitUsageStatus> result = new ArrayList<>();
        for (BenefitRow row : benefitRows) {
            if (!isOptionSelected(row, selectedOptionKeys)) {
                continue;
            }
            Long monthlyLimit = effectiveMonthlyLimit(row);
            long usedAmount = groupUsedAmount(row, benefitRows, usedAmountByBenefit);
            Long remainingLimit = (monthlyLimit == null)
                    ? null
                    : Math.max(0L, monthlyLimit - usedAmount);
            result.add(new BenefitUsageStatus(
                    row.getBenefitId(), row.getBenefitName(), row.getLimitGroupCode(),
                    usedAmount, monthlyLimit, remainingLimit, usageRate(monthlyLimit, usedAmount),
                    "Y".equals(row.getRequirePerformance())));
        }
        return result;
    }

    /**
     * 그달에 고른 선택지인가. 선택형이 아닌 혜택(묶음 코드 없음)은 항상 참이다.
     *
     * 선택 기록이 없는 묶음은 고르지 않은 것이라 하나도 담기지 않는다. 기본값으로 아무거나 켜면
     * 회원이 고르지 않은 혜택이 화면에 뜨고, 그 카드로 결제해도 실제로는 적용되지 않는다.
     */
    private boolean isOptionSelected(BenefitRow row, Map<String, String> selectedOptionKeys) {
        if (row.getOptionGroupCode() == null) {
            return true;
        }
        return row.getOptionKey() != null
                && row.getOptionKey().equals(selectedOptionKeys.get(row.getOptionGroupCode()));
    }

    /**
     * 이용률(%) — 소진액 ÷ 한도. 한도가 없거나(null) 0이면 정의되지 않아 null이다.
     * 0을 돌려주면 "아직 안 썼다"로 읽혀 "쓸 한도가 없다"와 구분되지 않는다.
     *
     * <b>100을 넘지 않게 자른다.</b> 소진액이 한도를 넘는 상태는 엔진이 만들 수 없지만(계산기가
     * 한도에서 클램프한다), 손으로 넣은 데이터에는 있을 수 있다. 그때 잔여는 0으로 깎으면서
     * 이용률만 116%로 내보내면 응답이 자기모순이고, 화면 진행 막대도 칸을 넘친다.
     */
    private BigDecimal usageRate(Long monthlyLimit, long usedAmount) {
        if (monthlyLimit == null || monthlyLimit == 0) {
            return null;
        }
        BigDecimal rate = BigDecimal.valueOf(usedAmount * 100)
                .divide(BigDecimal.valueOf(monthlyLimit), 1, RoundingMode.HALF_UP);
        return rate.min(MAX_USAGE_RATE);
    }

    /** 판정된 구간의 개별한도가 있으면 그 값, 없으면 base 월 한도. 둘 다 없으면 null(한도 없음). */
    private Long effectiveMonthlyLimit(BenefitRow row) {
        return row.getTierMonthlyLimit() != null ? row.getTierMonthlyLimit() : row.getMonthlyLimit();
    }

    /**
     * 월 한도 표시에 쓸 소진액. 묶음이면 같은 코드를 가진 혜택들의 소진액 합이다.
     * (CardBenefitSelector의 계산용 그룹 합산과 같은 규칙 — 표시와 계산이 어긋나지 않게 맞춘다.)
     *
     * <b>합산 범위는 고르지 않은 선택지까지 포함한 전체 행이다.</b> 계산기도 같은 범위로 합산한다 —
     * 한도 묶음이 선택지 경계를 가로지르는 카드가 있어(같은 쇼핑 한도를 여러 선택지가 공유),
     * 표시만 고른 선택지로 좁히면 같은 지갑의 잔액이 화면과 계산에서 다르게 나온다.
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
