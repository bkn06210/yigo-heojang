package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.CardBenefitSelection;
import com.wallet.engine.model.SimulatedCard;
import com.wallet.engine.model.SimulatedPayment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 결제를 카드에 나누는 방법들 — 비교 실험용.
 *
 * 포트폴리오 시뮬레이터는 결제마다 그 순간 혜택이 가장 큰 카드를 고른다(탐욕 선택). 그 결과가
 * 가능한 배분 중 가장 좋은 것과 얼마나 다른지 재려면 기준이 필요해서, 여기서 모든 배분을 직접 돌려 본다.
 *
 * 소진 누적은 시뮬레이터와 같은 {@link CardRunState}를 쓴다. 누적 규칙을 따로 적으면 차이가 났을 때
 * 배분 방식 때문인지 누적 규칙 때문인지 가를 수 없다.
 */
final class PortfolioAllocations {

    /** 결제 한 건을 어느 카드로 낼지 고를 때 카드에 매기는 점수 */
    @FunctionalInterface
    interface Scoring {
        double scoreOf(SimulatedCard card, CardBenefitSelection selection);
    }

    /** 시뮬레이터와 같은 기준 — 지금 받는 혜택액만 본다 */
    static final Scoring IMMEDIATE_BENEFIT = (card, selection) -> selection.benefitAmount();

    /**
     * 실험용 기준 — 지금 받는 혜택액에 스탬프 한 칸의 값을 더한다.
     *
     * 스탬프형은 N번째 결제에서만 돈이 나오므로 그 전 결제는 혜택액이 0원으로 보인다. 그래서 1원이라도
     * 주는 다른 카드에 매번 밀려 스탬프가 영영 차지 않는다. 한 칸을 "지급액 ÷ N"원어치로 쳐 주면
     * 채우는 중인 스탬프를 버리지 않게 된다. 이번 결제에서 지급되는 스탬프는 혜택액에 이미 들어 있어 더하지 않는다.
     */
    static final Scoring WITH_STAMP_PROGRESS = (card, selection) -> {
        double score = selection.benefitAmount();
        for (Long stampedBenefitId : selection.stampedBenefitIds()) {
            boolean paidNow = stampedBenefitId.equals(selection.benefitId()) && selection.benefitAmount() > 0;
            if (paidNow) {
                continue;
            }
            for (BenefitCandidate candidate : card.candidates()) {
                if (candidate.getBenefitId() == stampedBenefitId
                        && candidate.getRule().getCalcMethod() == CalcMethod.COUNT_STEP) {
                    score += candidate.getRule().getBenefitValue().doubleValue() / candidate.getRule().getStepCount();
                }
            }
        }
        return score;
    };

    private PortfolioAllocations() {
    }

    /** 결제마다 점수가 가장 높은 카드로 낸다. 동점이면 cardId가 작은 카드 */
    static long greedyTotal(List<SimulatedCard> cards, List<SimulatedPayment> payments, Scoring scoring) {
        if (cards.isEmpty() || payments.isEmpty()) {
            return 0L;
        }
        CardBenefitSelector selector = new CardBenefitSelector();
        List<SimulatedPayment> ordered = inDateOrder(payments);
        CardRunState[] states = newStates(cards);

        long total = 0L;
        LocalDate currentDay = null;
        for (SimulatedPayment payment : ordered) {
            if (!payment.paymentDate().equals(currentDay)) {
                resetDaily(states);
                currentDay = payment.paymentDate();
            }
            int winner = -1;
            double winningScore = 0;
            CardBenefitSelection winningSelection = null;
            for (int index = 0; index < cards.size(); index++) {
                SimulatedCard card = cards.get(index);
                CardBenefitSelection selection =
                        selector.select(card.candidates(), states[index].toCardState(), payment.request());
                double score = scoring.scoreOf(card, selection);
                boolean beats = winner < 0
                        || score > winningScore
                        || (score == winningScore && card.cardId() < cards.get(winner).cardId());
                if (beats) {
                    winner = index;
                    winningScore = score;
                    winningSelection = selection;
                }
            }
            total += states[winner].apply(winningSelection, cards.get(winner).candidates());
        }
        return total;
    }

    /**
     * 가능한 모든 배분 중 총 혜택이 가장 큰 값. 결제 n건을 카드 k장에 나누는 k^n가지를 전부 돌린다.
     *
     * 카드 안에서 어느 혜택이 적용될지는 고르지 않는다 — 그건 카드사 규칙(혜택액 최대 1개)이지
     * 회원의 선택이 아니다. 회원이 고를 수 있는 것은 "이 결제를 어느 카드로 낼까"뿐이다.
     */
    static long optimalTotal(List<SimulatedCard> cards, List<SimulatedPayment> payments) {
        if (cards.isEmpty() || payments.isEmpty()) {
            return 0L;
        }
        List<SimulatedPayment> ordered = inDateOrder(payments);
        int[] cardIndexOfPayment = new int[ordered.size()];
        long best = 0L;
        while (true) {
            best = Math.max(best, totalOf(cards, ordered, cardIndexOfPayment));
            int position = 0;
            while (position < cardIndexOfPayment.length && cardIndexOfPayment[position] == cards.size() - 1) {
                cardIndexOfPayment[position] = 0;
                position += 1;
            }
            if (position == cardIndexOfPayment.length) {
                return best;
            }
            cardIndexOfPayment[position] += 1;
        }
    }

    /** 정해진 배분대로 결제했을 때의 총 혜택 */
    private static long totalOf(List<SimulatedCard> cards, List<SimulatedPayment> ordered,
                                int[] cardIndexOfPayment) {
        CardBenefitSelector selector = new CardBenefitSelector();
        CardRunState[] states = newStates(cards);
        long total = 0L;
        LocalDate currentDay = null;
        for (int index = 0; index < ordered.size(); index++) {
            SimulatedPayment payment = ordered.get(index);
            if (!payment.paymentDate().equals(currentDay)) {
                resetDaily(states);
                currentDay = payment.paymentDate();
            }
            int cardIndex = cardIndexOfPayment[index];
            SimulatedCard card = cards.get(cardIndex);
            CardBenefitSelection selection =
                    selector.select(card.candidates(), states[cardIndex].toCardState(), payment.request());
            total += states[cardIndex].apply(selection, card.candidates());
        }
        return total;
    }

    private static List<SimulatedPayment> inDateOrder(List<SimulatedPayment> payments) {
        List<SimulatedPayment> ordered = new ArrayList<>(payments);
        ordered.sort(Comparator.comparing(SimulatedPayment::paymentDate));
        return ordered;
    }

    private static CardRunState[] newStates(List<SimulatedCard> cards) {
        CardRunState[] states = new CardRunState[cards.size()];
        for (int index = 0; index < cards.size(); index++) {
            SimulatedCard card = cards.get(index);
            states[index] = new CardRunState(card.monthStatus(), card.quarterStatus(), card.selectedOptionKeys());
        }
        return states;
    }

    private static void resetDaily(CardRunState[] states) {
        for (CardRunState state : states) {
            state.resetDaily();
        }
    }
}
