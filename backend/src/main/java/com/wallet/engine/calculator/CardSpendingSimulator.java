package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.CardBenefitSelection;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.SimulatedPayment;
import com.wallet.engine.model.SpendingSimulation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 카드 한 장으로 소비 목록을 결제했다면 얼마를 받았을지 계산한다.
 *
 * <b>왜 필요한가</b> — 아직 쓰지 않은 카드의 월 혜택을 구해야 하는데, 월 총액에 요율을 곱하면 틀린다:
 *
 * <pre>
 * 카페 월 20만원 × 10%  =  20,000원   ← 틀림
 * 5천원 40번 (월 5회 한도)  =  2,500원   ← 실제 값
 * </pre>
 *
 * 한도는 결제 <b>건수</b>에 따라 걸리므로 총액만으로는 구할 수 없다. 그래서 결제 계산기를
 * 그대로 쓰되 소진 상태를 DB가 아니라 메모리에서 누적한다({@link CardRunState}).
 *
 * 실적 구간을 어느 것으로 볼지는 호출자가 정해 넘긴다 — 이 계산기는 카드 보유 여부를 모른다.
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
     * @param monthStatus        전월 실적으로 판정한 구간
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

        CardRunState state = new CardRunState(monthStatus, quarterStatus, selectedOptionKeys);
        LocalDate currentDay = null;

        List<SimulatedPayment> ordered = new ArrayList<>(payments);
        ordered.sort(Comparator.comparing(SimulatedPayment::paymentDate));

        for (SimulatedPayment payment : ordered) {
            if (!payment.paymentDate().equals(currentDay)) {
                state.resetDaily();
                currentDay = payment.paymentDate();
            }
            CardBenefitSelection selection =
                    selector.select(candidates, state.toCardState(), payment.request());
            state.apply(selection, candidates);
        }
        return new SpendingSimulation(state.totalBenefitAmount(), state.breakdowns());
    }
}
