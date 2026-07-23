package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.BenefitResult;
import com.wallet.engine.model.BenefitUsage;
import com.wallet.engine.model.CalcContext;
import com.wallet.engine.model.CardBenefitSelection;
import com.wallet.engine.model.CardState;
import com.wallet.engine.model.PaymentRequest;

import java.util.List;

/**
 * 카드 한 장의 계산기 — 이 카드가 이번 결제에서 줄 수 있는 최대 혜택을 구한다.
 *
 * 흐름:
 * <pre>
 * 1. 매칭   이번 결제에 해당하는 혜택만 남긴다 (BenefitMatcher)
 * 2. 소진   혜택별 소진액을 조립한다. 묶음(limit_group_code)이면 그룹 합산액
 * 3. 계산   혜택마다 BenefitCalculator로 금액을 구한다
 * 4. 선택   적용된 것 중 혜택액 최대 1개. 동점이면 benefit_id 오름차순
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
        for (BenefitCandidate candidate : candidates) {
            if (!matcher.matches(candidate, request)) {
                continue;
            }
            BenefitResult result = calculator.calculate(
                    candidate.getRule(), toCalcContext(candidate, candidates, cardState, request));
            if (!result.applied()) {
                continue;
            }
            if (beats(result, candidate, best)) {
                best = new CardBenefitSelection(
                        candidate.getBenefitId(),
                        candidate.getRule().getBenefitKind(),
                        result.benefitAmount(),
                        result.estimate(),
                        result.appliedCap());
            }
        }
        return best;
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
        BenefitUsage usage = cardState.usageOf(candidate.getBenefitId());
        return CalcContext.builder()
                .paymentAmount(request.paymentAmount())
                .usedPointAmount(request.usedPointAmount())
                .paymentType(request.paymentType())
                .amountEstimated(request.amountEstimated())
                .performanceMet(cardState.performanceMet())
                .monthlyUsedAmount(groupMonthlyUsedAmount(candidate, allCandidates, cardState))
                .monthlyUsedCount(usage.monthlyUsedCount())
                .dailyUsedAmount(usage.dailyUsedAmount())
                .dailyUsedCount(usage.dailyUsedCount())
                .sharedMonthlyLimit(cardState.sharedMonthlyLimit())
                .sharedLimitUsed(cardState.sharedLimitUsed())
                .build();
    }

    /**
     * 월 한도 판정에 쓸 소진액. 묶음이면 같은 코드를 가진 혜택들의 소진액 합이다.
     * 묶지 않으면 한도가 그룹 혜택 수만큼 배로 새는데, 에러 없이 금액만 틀리므로 눈에 안 띈다.
     *
     * 횟수 한도(monthly_count_limit)는 합산하지 않는다 — 스키마상 한도를 공유하는 건 monthly_limit뿐이다.
     */
    private long groupMonthlyUsedAmount(BenefitCandidate candidate, List<BenefitCandidate> allCandidates,
                                        CardState cardState) {
        String groupCode = candidate.getLimitGroupCode();
        if (groupCode == null) {
            return cardState.usageOf(candidate.getBenefitId()).monthlyUsedAmount();
        }
        return allCandidates.stream()
                .filter(other -> groupCode.equals(other.getLimitGroupCode()))
                .mapToLong(other -> cardState.usageOf(other.getBenefitId()).monthlyUsedAmount())
                .sum();
    }
}
