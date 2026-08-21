package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.BenefitUsage;
import com.wallet.engine.model.CardBenefitSelection;
import com.wallet.engine.model.CardState;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.SpendingSimulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 시뮬레이션 중인 카드 한 장의 누적 상태 — 통합한도 소진과 혜택별 소진.
 *
 * <b>누적 규칙을 한 곳에 모으려고 분리했다.</b> 카드 한 장을 도는 시뮬레이터와 여러 장을 도는
 * 시뮬레이터가 각자 누적하면 규칙이 두 벌이 되고, 한쪽만 고쳐지면 같은 소비에 다른 답이 나온다.
 *
 * 규칙은 정산(SettlementService)이 DB에 가산하는 것과 같다.
 * <pre>
 * 혜택액 0원  소진을 올리지 않는다 (횟수 헛소비 방지)
 * 스탬프      금액이 0원이어도 횟수는 올린다 (안 올리면 N회째가 영영 오지 않는다)
 * 일 소진     날짜가 바뀌면 리셋한다
 * </pre>
 *
 * 실적 판정 결과는 시뮬레이션 내내 고정이다 — 전월(전분기) 기준이라 이번 달 결제로 바뀌지 않는다.
 */
final class CardRunState {

    private final PerformanceStatus monthStatus;
    private final PerformanceStatus quarterStatus;
    private final Map<String, String> selectedOptionKeys;
    private final Map<Long, Accumulator> accumulators = new LinkedHashMap<>();

    private long sharedLimitUsed;
    private long totalBenefitAmount;
    private int appliedPaymentCount;

    CardRunState(PerformanceStatus monthStatus, PerformanceStatus quarterStatus,
                 Map<String, String> selectedOptionKeys) {
        if (monthStatus == null) {
            throw new IllegalArgumentException("전월 실적 판정 결과는 필수다");
        }
        this.monthStatus = monthStatus;
        this.quarterStatus = quarterStatus;
        this.selectedOptionKeys = selectedOptionKeys == null ? Map.of() : selectedOptionKeys;
    }

    /** 지금까지의 소진을 반영한 계산 입력 */
    CardState toCardState() {
        return new CardState(monthStatus, quarterStatus, sharedLimitUsed, snapshot(), selectedOptionKeys);
    }

    /**
     * 이 결제로 확정된 혜택을 소진에 반영한다.
     *
     * @return 실제로 받은 혜택액. 한도 소진으로 0원이면 0
     */
    long apply(CardBenefitSelection selection, List<BenefitCandidate> candidates) {
        long discount = selection.benefitAmount();
        // 혜택이 뽑혔어도 한도 소진으로 0원이면 받은 혜택이 없다 — 정산과 같은 판정이다
        Long appliedBenefitId = (selection.hasBenefit() && discount > 0) ? selection.benefitId() : null;

        for (Long stampedId : selection.stampedBenefitIds()) {
            boolean paidThisTime = stampedId.equals(appliedBenefitId);
            accumulator(stampedId).add(paidThisTime ? discount : 0L);
        }
        if (appliedBenefitId != null && !selection.stampedBenefitIds().contains(appliedBenefitId)) {
            accumulator(appliedBenefitId).add(discount);
        }
        if (appliedBenefitId != null && usesSharedLimit(candidates, appliedBenefitId)) {
            sharedLimitUsed += discount;
        }
        if (discount > 0) {
            appliedPaymentCount += 1;
        }
        totalBenefitAmount += discount;
        return discount;
    }

    void resetDaily() {
        accumulators.values().forEach(Accumulator::resetDaily);
    }

    long totalBenefitAmount() {
        return totalBenefitAmount;
    }

    int appliedPaymentCount() {
        return appliedPaymentCount;
    }

    /** 혜택별 내역 — 금액이 큰 순서, 동점이면 benefit_id 오름차순 */
    List<SpendingSimulation.BenefitBreakdown> breakdowns() {
        List<SpendingSimulation.BenefitBreakdown> breakdowns = new ArrayList<>();
        accumulators.forEach((benefitId, acc) ->
                breakdowns.add(new SpendingSimulation.BenefitBreakdown(benefitId, acc.amount, acc.count)));
        breakdowns.sort(Comparator
                .comparingLong(SpendingSimulation.BenefitBreakdown::benefitAmount).reversed()
                .thenComparingLong(SpendingSimulation.BenefitBreakdown::benefitId));
        return breakdowns;
    }

    /**
     * 한 달 시뮬레이션이라 분기·연 소진은 월 소진과 같은 값이다.
     *
     * 이 카드를 이번 달에 처음 쓴다고 보기 때문이다. 미보유 카드에는 이전 달 소진이 없어 이 전제가 맞다.
     */
    private List<BenefitUsage> snapshot() {
        List<BenefitUsage> usages = new ArrayList<>(accumulators.size());
        accumulators.forEach((benefitId, acc) -> usages.add(new BenefitUsage(
                benefitId, acc.amount, acc.count, acc.dailyAmount, acc.dailyCount,
                acc.amount, acc.count, acc.amount, acc.count)));
        return usages;
    }

    private Accumulator accumulator(long benefitId) {
        return accumulators.computeIfAbsent(benefitId, id -> new Accumulator());
    }

    private boolean usesSharedLimit(List<BenefitCandidate> candidates, long benefitId) {
        return candidates.stream()
                .filter(candidate -> candidate.getBenefitId() == benefitId)
                .findFirst()
                .map(candidate -> candidate.getRule().isUseSharedLimit())
                .orElse(false);
    }

    /** 혜택 하나의 소진 누적기. user_benefit_usage 한 행에 대응한다 */
    private static final class Accumulator {
        private long amount;
        private int count;
        private long dailyAmount;
        private int dailyCount;

        void add(long discount) {
            amount += discount;
            count += 1;
            dailyAmount += discount;
            dailyCount += 1;
        }

        void resetDaily() {
            dailyAmount = 0L;
            dailyCount = 0;
        }
    }
}
