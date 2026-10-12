package com.wallet.engine.assembler;

import com.wallet.engine.calculator.TierLimitResolver;
import com.wallet.engine.dao.dto.BenefitExclusionRow;
import com.wallet.engine.dao.dto.BenefitRow;
import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.BenefitExclusion;
import com.wallet.engine.model.BenefitKind;
import com.wallet.engine.model.BenefitRule;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.ExclusionType;
import com.wallet.engine.model.PerformancePeriod;
import com.wallet.engine.model.TargetType;
import com.wallet.engine.model.TierLimitOverride;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * benefit 조회 row → BenefitCandidate 변환기.
 *
 * DB 원값의 '해석'을 이 한 곳에 모은다 — 계산기 모델은 손대지 않는다.
 *   · 'Y'/'N' → boolean (requirePerformance, useSharedLimit)
 *   · benefit_kind·calc_method·target_type·exclusion_type 문자열 → enum (모르는 값은 valueOf가 예외)
 *   · benefit_tier_limit 조인값(tierMonthlyLimit·tierBenefitValue) → TierLimitResolver로 구간 유효값 반영
 *   · 카드 전체 예외(card_benefit_exclusion) → 그 카드의 혜택마다 붙여 혜택별 예외와 한 목록으로
 *
 * 상한·조건 컬럼의 Long/Integer는 그대로 넘긴다 — NULL(제약 없음)≠0(혜택 없음)을 유지한다.
 * 매칭·계산에 쓰이지 않는 benefit_name은 여기서 버린다(추천 응답 문구는 서비스가 별도로 조회한 이름을 쓴다).
 *
 * enum 변환에 from()이 아니라 valueOf를 쓰는 이유: enum 상수명과 DB 값이 1:1로 같고,
 * 오타난 값은 valueOf가 IllegalArgumentException을 던져 스키마 위반이 드러나기 때문이다.
 */
@Component
public class BenefitCandidateAssembler {

    private final TierLimitResolver tierLimitResolver = new TierLimitResolver();

    public List<BenefitCandidate> toCandidates(List<BenefitRow> benefitRows,
                                               List<BenefitExclusionRow> exclusionRows) {
        Map<Long, List<BenefitExclusion>> exclusionsByBenefit =
                attachToBenefits(benefitRows, exclusionRows, this::toExclusion);

        return benefitRows.stream()
                .map(row -> toCandidate(row, exclusionsByBenefit.get(row.getBenefitId())))
                .toList();
    }

    /**
     * 혜택마다 걸리는 예외 행 — 그 혜택 자신의 예외에, 같은 카드의 카드 전체 예외를 더한 것.
     *
     * 후보로 조립하지 않고 예외만 훑어보려는 쪽(브리핑의 제외 업종 대조)이 쓴다. 카드 전체 예외를
     * 붙이는 규칙을 {@link #toCandidates}와 같은 코드로 태우기 위해 여기 둔다 — 호출하는 쪽마다
     * 따로 붙이면 한쪽만 카드 전체 예외를 빠뜨려도 드러나지 않는다.
     *
     * @param benefitRows   혜택 행. 카드 id가 채워져 있어야 한다
     * @param exclusionRows 혜택별 예외와 카드 전체 예외가 섞인 조회 결과
     * @return 혜택 id → 그 혜택에 걸리는 예외 행. 예외가 없는 혜택도 빈 목록으로 담긴다
     */
    public Map<Long, List<BenefitExclusionRow>> exclusionRowsByBenefit(List<BenefitRow> benefitRows,
                                                                      List<BenefitExclusionRow> exclusionRows) {
        return attachToBenefits(benefitRows, exclusionRows, Function.identity());
    }

    /**
     * 예외 행을 혜택마다 나눠 붙인다. 혜택별 예외는 그 혜택에, 카드 전체 예외는 같은 카드의 모든 혜택에.
     *
     * 카드 전체 예외는 조회에서 한 줄로 온다. 조회에서 혜택 수만큼 펼치면 (카드 예외 수 × 혜택 수)행을
     * 읽어 객체로 바꿔야 해서, 펼치는 일을 여기서 한다. 붙인 뒤에는 두 종류를 구분하지 않는다 —
     * 예외를 판정하는 쪽(BenefitMatcher)이 "카드 예외"를 따로 알게 되면 판정 규칙이 두 벌이 된다.
     *
     * 카드 id로 맞춰 붙이므로 여러 카드의 행을 한꺼번에 넘겨도 다른 카드의 예외가 섞이지 않는다.
     * 변환은 행마다 한 번만 한다. 카드 전체 예외는 변환한 값 하나를 그 카드의 혜택들이 함께 쓴다.
     *
     * @param converter 예외 행을 담을 모양으로 바꾸는 함수
     */
    private <T> Map<Long, List<T>> attachToBenefits(List<BenefitRow> benefitRows,
                                                    List<BenefitExclusionRow> exclusionRows,
                                                    Function<BenefitExclusionRow, T> converter) {
        Map<Long, List<T>> cardWideByCard = new HashMap<>();
        Map<Long, List<T>> ownByBenefit = new HashMap<>();
        for (BenefitExclusionRow row : exclusionRows) {
            T converted = converter.apply(row);
            if (row.isCardWide()) {
                cardWideByCard.computeIfAbsent(row.getCardId(), unused -> new ArrayList<>()).add(converted);
            } else {
                ownByBenefit.computeIfAbsent(row.getBenefitId(), unused -> new ArrayList<>()).add(converted);
            }
        }

        Map<Long, List<T>> attached = new HashMap<>();
        for (BenefitRow benefit : benefitRows) {
            if (benefit.getCardId() <= 0) {
                // 카드 id 없이는 카드 전체 예외를 붙일 곳을 찾지 못한다. 그대로 두면 예외가 조용히
                // 빠져 받을 수 없는 혜택이 계산되므로, 조회가 card_id를 빠뜨린 것을 드러낸다.
                throw new IllegalArgumentException(
                        "혜택 행에 카드 id가 없다: benefitId=" + benefit.getBenefitId());
            }
            List<T> own = ownByBenefit.getOrDefault(benefit.getBenefitId(), List.of());
            List<T> cardWide = cardWideByCard.getOrDefault(benefit.getCardId(), List.of());

            List<T> combined = new ArrayList<>(own.size() + cardWide.size());
            combined.addAll(own);
            combined.addAll(cardWide);
            attached.put(benefit.getBenefitId(), combined);
        }
        return attached;
    }

    private BenefitCandidate toCandidate(BenefitRow row, List<BenefitExclusion> exclusions) {
        // 구간별 개별한도·혜택값을 먼저 반영해 계산기가 상속을 몰라도 되게 만든다
        BenefitRule rule = tierLimitResolver.resolve(toBaseRule(row), new TierLimitOverride(
                row.getTierMonthlyLimit(),
                row.getTierQuarterlyLimit(),
                row.getTierYearlyLimit(),
                row.getTierBenefitValue()));

        return BenefitCandidate.builder()
                .targetType(TargetType.valueOf(row.getTargetType()))
                .targetCategoryId(row.getTargetCategoryId())
                .targetMerchantId(row.getTargetMerchantId())
                .limitGroupCode(row.getLimitGroupCode())
                .countGroupCode(row.getCountGroupCode())
                .optionGroupCode(row.getOptionGroupCode())
                .optionKey(row.getOptionKey())
                .performancePeriod(PerformancePeriod.from(row.getPerformancePeriod()))
                .exclusions(exclusions)
                .rule(rule)
                .build();
    }

    private BenefitRule toBaseRule(BenefitRow row) {
        return BenefitRule.builder()
                .benefitId(row.getBenefitId())
                .benefitKind(BenefitKind.valueOf(row.getBenefitKind()))
                .calcMethod(CalcMethod.valueOf(row.getCalcMethod()))
                .benefitValue(row.getBenefitValue())
                // COUNT_STEP은 이 값이 없으면 규칙을 만들 수 없다(몇 회마다 지급인지 모른다)
                .stepCount(row.getStepCount())
                .requirePerformance("Y".equals(row.getRequirePerformance()))
                .requirePaymentType(row.getRequirePaymentType())
                .minTxnAmount(row.getMinTxnAmount())
                .maxEligibleAmount(row.getMaxEligibleAmount())
                .maxBenefitPerTxn(row.getMaxBenefitPerTxn())
                .monthlyLimit(row.getMonthlyLimit())
                .dailyLimit(row.getDailyLimit())
                .quarterlyLimit(row.getQuarterlyLimit())
                .yearlyLimit(row.getYearlyLimit())
                .monthlyCountLimit(row.getMonthlyCountLimit())
                .dailyCountLimit(row.getDailyCountLimit())
                .quarterlyCountLimit(row.getQuarterlyCountLimit())
                .yearlyCountLimit(row.getYearlyCountLimit())
                .useSharedLimit("Y".equals(row.getUseSharedLimit()))
                .build();
    }

    private BenefitExclusion toExclusion(BenefitExclusionRow row) {
        return new BenefitExclusion(
                ExclusionType.valueOf(row.getExclusionType()),
                row.getExclusionValue());
    }
}
