package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.BenefitUsage;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.CardBenefitSelection;
import com.wallet.engine.model.CardState;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.SimulatedPayment;
import com.wallet.engine.model.SpendingSimulation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 카드 한 장으로 소비 목록을 결제했다면 얼마를 받았을지 계산한다.
 *
 * <b>왜 필요한가</b> — "내 소비에 맞는 카드"에 답하려면 아직 쓰지 않은 카드의 혜택을 구해야 하는데,
 * 그 카드에는 소진 이력이 없다. 그렇다고 월 총액에 요율을 곱하면 틀린다:
 *
 * <pre>
 * 카페 월 20만원 × 10%  =  20,000원   ← 틀림
 * 5천원 40번            =   5,000원   ← 건당 최대·횟수·통합한도가 걸린 실제 값
 * </pre>
 *
 * 한도는 결제 <b>건수</b>에 따라 걸리므로 총액만으로는 구할 수 없다. 그래서 결제 계산기를
 * 그대로 쓰되 소진 상태를 DB가 아니라 메모리에서 누적한다.
 *
 * <b>누적 규칙은 정산과 같아야 한다.</b> 실제 결제 때 상태를 가산하는 규칙과 어긋나면
 * "추천에서는 12,000원이라 했는데 한 달 써보니 8,000원"이 된다. 그래서 아래 셋을 그대로 따른다.
 * <pre>
 * 혜택액 0원  소진을 올리지 않는다 (횟수 헛소비 방지)
 * 스탬프      금액이 0원이어도 횟수는 올린다 (안 올리면 N회째가 영영 오지 않는다)
 * 일 소진     날짜가 바뀌면 리셋한다
 * </pre>
 *
 * 실적 구간은 시뮬레이션 내내 고정이다 — 실적 판정은 전월(전분기) 기준이라 이번 달 결제로
 * 바뀌지 않는다. 어느 구간으로 볼지(미보유 카드면 발급 초기 유예 구간)는 호출자가 정해 넘긴다.
 *
 * Spring·DB·시계에 의존하지 않는 순수 계산기다.
 */
public final class CardSpendingSimulator {

    private final CardBenefitSelector selector;

    public CardSpendingSimulator() {
        this(new CardBenefitSelector());
    }

    public CardSpendingSimulator(CardBenefitSelector selector) {
        this.selector = selector;
    }

    /**
     * 소비 목록을 이 카드로 결제했다고 보고 혜택을 누적한다.
     *
     * @param candidates         이 카드의 활성 혜택 <b>전부</b>. 매칭될 것만 넘기면 묶음 한도 합산이 틀린다
     * @param monthStatus        전월 실적으로 판정한 구간 (미보유 카드면 유예 구간)
     * @param quarterStatus      전분기 축 판정 결과. 분기 구간표가 없는 카드면 null
     * @param selectedOptionKeys 선택형 혜택 묶음별로 고른 선택지. 안 고른 묶음의 혜택은 적용되지 않는다
     * @param payments           흘려보낼 결제 목록. 날짜순으로 정렬해 처리한다
     */
    public SpendingSimulation simulate(List<BenefitCandidate> candidates,
                                       PerformanceStatus monthStatus,
                                       PerformanceStatus quarterStatus,
                                       Map<String, String> selectedOptionKeys,
                                       List<SimulatedPayment> payments) {
        if (candidates == null || monthStatus == null) {
            throw new IllegalArgumentException("혜택 목록과 전월 실적 판정 결과는 필수다");
        }
        if (payments == null || payments.isEmpty()) {
            return SpendingSimulation.empty();
        }

        Map<Long, Accumulator> accumulators = new LinkedHashMap<>();
        long sharedLimitUsed = 0L;
        long total = 0L;
        LocalDate currentDay = null;

        List<SimulatedPayment> ordered = new ArrayList<>(payments);
        ordered.sort(Comparator.comparing(SimulatedPayment::paymentDate));

        for (SimulatedPayment payment : ordered) {
            if (!payment.paymentDate().equals(currentDay)) {
                accumulators.values().forEach(Accumulator::resetDaily);
                currentDay = payment.paymentDate();
            }

            CardState state = new CardState(monthStatus, quarterStatus, sharedLimitUsed,
                    snapshot(accumulators), selectedOptionKeys);
            CardBenefitSelection selection = selector.select(candidates, state, payment.request());

            long discount = selection.benefitAmount();
            // 혜택이 뽑혔어도 한도 소진으로 0원이면 받은 혜택이 없다 — 정산과 같은 판정이다
            Long appliedBenefitId = (selection.hasBenefit() && discount > 0) ? selection.benefitId() : null;

            for (Long stampedId : selection.stampedBenefitIds()) {
                boolean paidThisTime = stampedId.equals(appliedBenefitId);
                accumulator(accumulators, stampedId).add(paidThisTime ? discount : 0L);
            }
            if (appliedBenefitId != null && !selection.stampedBenefitIds().contains(appliedBenefitId)) {
                accumulator(accumulators, appliedBenefitId).add(discount);
            }
            if (appliedBenefitId != null && usesSharedLimit(candidates, appliedBenefitId)) {
                sharedLimitUsed += discount;
            }
            total += discount;
        }

        List<SpendingSimulation.BenefitBreakdown> breakdowns = new ArrayList<>();
        accumulators.forEach((benefitId, acc) ->
                breakdowns.add(new SpendingSimulation.BenefitBreakdown(benefitId, acc.amount, acc.count)));
        breakdowns.sort(Comparator
                .comparingLong(SpendingSimulation.BenefitBreakdown::benefitAmount).reversed()
                .thenComparingLong(SpendingSimulation.BenefitBreakdown::benefitId));
        return new SpendingSimulation(total, breakdowns);
    }

    /**
     * 한 달 시뮬레이션이라 분기·연 소진은 월 소진과 같은 값이다.
     *
     * 이 카드를 이번 달에 처음 쓴다고 보기 때문이다. 분기·연 한도가 있는 혜택은 이전 달 소진이
     * 있으면 실제보다 후하게 나오는데, 미보유 카드에는 이전 달 소진이 없어 이 전제가 맞다.
     */
    private List<BenefitUsage> snapshot(Map<Long, Accumulator> accumulators) {
        List<BenefitUsage> usages = new ArrayList<>(accumulators.size());
        accumulators.forEach((benefitId, acc) -> usages.add(new BenefitUsage(
                benefitId, acc.amount, acc.count, acc.dailyAmount, acc.dailyCount,
                acc.amount, acc.count, acc.amount, acc.count)));
        return usages;
    }

    private Accumulator accumulator(Map<Long, Accumulator> accumulators, long benefitId) {
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
