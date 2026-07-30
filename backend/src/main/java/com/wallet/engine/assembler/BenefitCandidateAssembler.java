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
import com.wallet.engine.model.TargetType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * benefit 조회 row → BenefitCandidate 변환기.
 *
 * DB 원값의 '해석'을 이 한 곳에 모은다 — 계산기 모델은 손대지 않는다.
 *   · 'Y'/'N' → boolean (requirePerformance, useSharedLimit)
 *   · benefit_kind·calc_method·target_type·exclusion_type 문자열 → enum (모르는 값은 valueOf가 예외)
 *   · benefit_tier_limit 조인값(tierMonthlyLimit·tierBenefitValue) → TierLimitResolver로 구간 유효값 반영
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
        Map<Long, List<BenefitExclusion>> exclusionsByBenefit = exclusionRows.stream()
                .collect(Collectors.groupingBy(
                        BenefitExclusionRow::getBenefitId,
                        Collectors.mapping(this::toExclusion, Collectors.toList())));

        return benefitRows.stream()
                .map(row -> toCandidate(row, exclusionsByBenefit.getOrDefault(row.getBenefitId(), List.of())))
                .toList();
    }

    private BenefitCandidate toCandidate(BenefitRow row, List<BenefitExclusion> exclusions) {
        // 구간별 개별한도·혜택값을 먼저 반영해 계산기가 상속을 몰라도 되게 만든다
        BenefitRule rule = tierLimitResolver.resolve(
                toBaseRule(row), row.getTierMonthlyLimit(), row.getTierBenefitValue());

        return BenefitCandidate.builder()
                .targetType(TargetType.valueOf(row.getTargetType()))
                .targetCategoryId(row.getTargetCategoryId())
                .targetMerchantId(row.getTargetMerchantId())
                .limitGroupCode(row.getLimitGroupCode())
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
                .requirePerformance("Y".equals(row.getRequirePerformance()))
                .requirePaymentType(row.getRequirePaymentType())
                .minTxnAmount(row.getMinTxnAmount())
                .maxEligibleAmount(row.getMaxEligibleAmount())
                .maxBenefitPerTxn(row.getMaxBenefitPerTxn())
                .monthlyLimit(row.getMonthlyLimit())
                .dailyLimit(row.getDailyLimit())
                .monthlyCountLimit(row.getMonthlyCountLimit())
                .dailyCountLimit(row.getDailyCountLimit())
                .useSharedLimit("Y".equals(row.getUseSharedLimit()))
                .build();
    }

    private BenefitExclusion toExclusion(BenefitExclusionRow row) {
        return new BenefitExclusion(
                ExclusionType.valueOf(row.getExclusionType()),
                row.getExclusionValue());
    }
}
