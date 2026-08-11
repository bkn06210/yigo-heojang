package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.BenefitResult;
import com.wallet.engine.model.BenefitUsage;
import com.wallet.engine.model.CalcContext;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.CardBenefitSelection;
import com.wallet.engine.model.CardState;
import com.wallet.engine.model.PaymentRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * 카드 한 장의 계산기 — 이 카드가 이번 결제에서 줄 수 있는 최대 혜택을 구한다.
 *
 * 흐름:
 * <pre>
 * 1. 선택형 필터 그달에 고르지 않은 선택지의 혜택을 뺀다 (option_group_code)
 * 2. 매칭       이번 결제에 해당하는 혜택만 남긴다 (BenefitMatcher)
 * 3. 소진       혜택별 소진액·횟수를 조립한다. 묶음이면 그룹 합산값
 * 4. 계산       혜택마다 BenefitCalculator로 금액을 구한다
 * 5. 선택       적용된 것 중 혜택액 최대 1개. 동점이면 benefit_id 오름차순
 * </pre>
 *
 * 합산하지 않고 1개만 고르는 이유: 실제 약관은 "타 할인과 중복 불가"가 기본이고,
 * expense.applied_benefit_id가 단수라 저장 구조도 이미 이 전제 위에 있다.
 *
 * <b>candidates에는 이 카드의 활성 혜택을 전부 넣어야 한다.</b> 매칭된 것만 넘기면
 * 묶음 그룹 합산이 틀린다 — 지금 결제와 무관한 혜택도 같은 그룹 한도를 이미 갉아먹었을 수 있다
 * ("통신·공과금·마트 합쳐서 월 5천원"에서 마트 결제 중이어도 통신 소진분은 차감돼야 한다).
 */
public final class CardBenefitSelector {

    private final BenefitMatcher matcher;
    private final BenefitCalculator calculator;

    public CardBenefitSelector() {
        this(new BenefitMatcher(), new BenefitCalculator());
    }

    public CardBenefitSelector(BenefitMatcher matcher, BenefitCalculator calculator) {
        this.matcher = matcher;
        this.calculator = calculator;
    }

    public CardBenefitSelection select(List<BenefitCandidate> candidates, CardState cardState,
                                       PaymentRequest request) {
        if (candidates == null || cardState == null || request == null) {
            throw new IllegalArgumentException("candidates, cardState, request는 필수다");
        }

        CardBenefitSelection best = CardBenefitSelection.none();
        List<Long> stampedBenefitIds = new ArrayList<>();

        for (BenefitCandidate candidate : candidates) {
            if (!isOptionSelected(candidate, cardState)) {
                continue;
            }
            if (!matcher.matches(candidate, request)) {
                continue;
            }
            BenefitResult result = calculator.calculate(
                    candidate.getRule(), toCalcContext(candidate, candidates, cardState, request));
            if (!result.applied()) {
                continue;
            }
            // 스탬프는 그 결제에서 다른 혜택을 받았는지와 무관하게 찍힌다 — 선택 결과와 별개로 기록한다
            if (candidate.getRule().getCalcMethod() == CalcMethod.COUNT_STEP) {
                stampedBenefitIds.add(candidate.getBenefitId());
            }
            if (beats(result, candidate, best)) {
                best = new CardBenefitSelection(
                        candidate.getBenefitId(),
                        candidate.getRule().getBenefitKind(),
                        result.benefitAmount(),
                        result.estimate(),
                        result.appliedCap(),
                        List.of());
            }
        }
        return best.withStamped(stampedBenefitIds);
    }

    /**
     * 선택형 혜택은 그달에 고른 선택지만 적용된다.
     *
     * 고르지 않은 달은 그 묶음의 혜택이 하나도 적용되지 않는다 — 기본값으로 아무거나 켜면
     * 회원이 고르지 않은 혜택을 받은 것으로 기록되고, 다음 달 선택과도 어긋난다.
     */
    private boolean isOptionSelected(BenefitCandidate candidate, CardState cardState) {
        if (candidate.getOptionGroupCode() == null) {
            return true;
        }
        return candidate.getOptionKey() != null
                && candidate.getOptionKey().equals(cardState.selectedOptionKeyOf(candidate.getOptionGroupCode()));
    }

    /** 혜택액이 크면 이긴다. 같으면 benefit_id가 작은 쪽 — 테스트 재현성을 위한 규칙 */
    private boolean beats(BenefitResult result, BenefitCandidate candidate, CardBenefitSelection best) {
        if (!best.hasBenefit()) {
            return true;
        }
        if (result.benefitAmount() != best.benefitAmount()) {
            return result.benefitAmount() > best.benefitAmount();
        }
        return candidate.getBenefitId() < best.benefitId();
    }

    private CalcContext toCalcContext(BenefitCandidate candidate, List<BenefitCandidate> allCandidates,
                                      CardState cardState, PaymentRequest request) {
        return CalcContext.builder()
                .paymentAmount(request.paymentAmount())
                .usedPointAmount(request.usedPointAmount())
                .paymentType(request.paymentType())
                .amountEstimated(request.amountEstimated())
                // 실적 충족은 이 혜택의 기간 축으로 판정한다 — 한 카드가 전월 축과 전분기 축을 함께 쓸 수 있다
                .performanceMet(cardState.performanceMet(candidate.getPerformancePeriod()))
                .monthlyUsedAmount(amountOfGroup(candidate, allCandidates, cardState, BenefitUsage::monthlyUsedAmount))
                .dailyUsedAmount(amountOfGroup(candidate, allCandidates, cardState, BenefitUsage::dailyUsedAmount))
                .quarterlyUsedAmount(
                        amountOfGroup(candidate, allCandidates, cardState, BenefitUsage::quarterlyUsedAmount))
                .yearlyUsedAmount(amountOfGroup(candidate, allCandidates, cardState, BenefitUsage::yearlyUsedAmount))
                .monthlyUsedCount(countOfGroup(candidate, allCandidates, cardState, BenefitUsage::monthlyUsedCount))
                .dailyUsedCount(countOfGroup(candidate, allCandidates, cardState, BenefitUsage::dailyUsedCount))
                .quarterlyUsedCount(
                        countOfGroup(candidate, allCandidates, cardState, BenefitUsage::quarterlyUsedCount))
                .yearlyUsedCount(countOfGroup(candidate, allCandidates, cardState, BenefitUsage::yearlyUsedCount))
                .sharedMonthlyLimit(cardState.sharedMonthlyLimit())
                .sharedLimitUsed(cardState.sharedLimitUsed())
                .build();
    }

    /**
     * 금액 한도 판정에 쓸 소진액. 묶음이면 같은 limit_group_code를 가진 혜택들의 소진액 합이다.
     * 묶지 않으면 한도가 그룹 혜택 수만큼 배로 새는데, 에러 없이 금액만 틀리므로 눈에 안 띈다.
     *
     * 묶음은 "한 지갑을 나눠 쓰는 것"이므로 기간 축(일·월·분기·연) 전부에서 공유한다.
     */
    private long amountOfGroup(BenefitCandidate candidate, List<BenefitCandidate> allCandidates,
                               CardState cardState, ToLongFunction<BenefitUsage> field) {
        return sumGroup(candidate, allCandidates, cardState,
                candidate.getLimitGroupCode(), BenefitCandidate::getLimitGroupCode, field);
    }

    /**
     * 횟수 한도 판정에 쓸 소진 횟수. 묶음 기준이 count_group_code라는 점만 금액과 다르다.
     *
     * 금액 묶음을 그대로 쓰면 범위가 다를 때 좁은 쪽이 대상 수만큼 배로 샌다 —
     * "택시·커피·영화관 합쳐 월 5천원"에 "영화관 3사 합쳐 연 4회"가 겹치면 연 4회가 12회가 된다.
     */
    private int countOfGroup(BenefitCandidate candidate, List<BenefitCandidate> allCandidates,
                             CardState cardState, ToIntFunction<BenefitUsage> field) {
        return (int) sumGroup(candidate, allCandidates, cardState,
                candidate.getCountGroupCode(), BenefitCandidate::getCountGroupCode, field::applyAsInt);
    }

    /** 묶음 코드가 없으면 이 혜택 단독 소진, 있으면 같은 코드를 가진 혜택 전부의 합 */
    private long sumGroup(BenefitCandidate candidate, List<BenefitCandidate> allCandidates, CardState cardState,
                          String groupCode, Function<BenefitCandidate, String> groupCodeOf,
                          ToLongFunction<BenefitUsage> field) {
        if (groupCode == null) {
            return field.applyAsLong(cardState.usageOf(candidate.getBenefitId()));
        }
        return allCandidates.stream()
                .filter(other -> groupCode.equals(groupCodeOf.apply(other)))
                .mapToLong(other -> field.applyAsLong(cardState.usageOf(other.getBenefitId())))
                .sum();
    }
}
