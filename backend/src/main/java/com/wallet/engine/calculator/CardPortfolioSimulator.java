package com.wallet.engine.calculator;

import com.wallet.engine.model.CardBenefitSelection;
import com.wallet.engine.model.PortfolioSimulation;
import com.wallet.engine.model.SimulatedCard;
import com.wallet.engine.model.SimulatedPayment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 카드 여러 장을 들고 결제마다 최적 카드로 결제했다면 얼마를 받았을지 계산한다.
 *
 * 결제 화면이 하는 일(카드마다 계산해 가장 큰 것을 고름)을 한 달치 반복한 것이다.
 * 다른 점은 <b>고른 카드의 소진만 올린다</b>는 것 하나다 — 실제로 그 카드로만 결제했으니
 * 나머지 카드의 한도는 줄지 않는다. 전부 올리면 안 쓴 카드의 한도가 닳아 뒤쪽 결제가 과소 계산된다.
 *
 * <b>카드를 한 장 추가했을 때의 순증</b>은 두 번 돌려 빼서 구한다. 후보를 안 써도 되므로
 * 순증은 절대 음수가 되지 않는다 — "카페는 늘고 마트는 주는" 손해가 구조적으로 생기지 않는다.
 *
 * 얼마를 그 카드로 옮길지 가정할 필요도 없다. 카페에서만 이기면 카페 결제만 그 카드로 잡히고
 * 나머지는 원래 카드가 잡힌다. 배분을 계산이 정한다.
 *
 * Spring·DB·시계에 의존하지 않는 순수 계산기다.
 */
public final class CardPortfolioSimulator {

    private final CardBenefitSelector selector;

    public CardPortfolioSimulator() {
        this(new CardBenefitSelector());
    }

    public CardPortfolioSimulator(CardBenefitSelector selector) {
        this.selector = selector;
    }

    /**
     * 결제마다 혜택이 가장 큰 카드를 골라 누적한다.
     *
     * @param cards    들고 있다고 볼 카드 목록
     * @param payments 흘려보낼 결제 목록. 날짜순으로 정렬해 처리한다
     */
    public PortfolioSimulation simulate(List<SimulatedCard> cards, List<SimulatedPayment> payments) {
        if (cards == null || cards.isEmpty() || payments == null || payments.isEmpty()) {
            return PortfolioSimulation.empty();
        }

        Map<Long, CardRunState> states = new LinkedHashMap<>();
        for (SimulatedCard card : cards) {
            if (states.putIfAbsent(card.cardId(),
                    new CardRunState(card.monthStatus(), card.quarterStatus(), card.selectedOptionKeys())) != null) {
                throw new IllegalArgumentException("cardId가 중복됐다: " + card.cardId());
            }
        }

        List<SimulatedPayment> ordered = new ArrayList<>(payments);
        ordered.sort(Comparator.comparing(SimulatedPayment::paymentDate));

        LocalDate currentDay = null;
        long total = 0L;

        for (SimulatedPayment payment : ordered) {
            if (!payment.paymentDate().equals(currentDay)) {
                states.values().forEach(CardRunState::resetDaily);
                currentDay = payment.paymentDate();
            }

            SimulatedCard winner = null;
            CardBenefitSelection winningSelection = null;
            for (SimulatedCard card : cards) {
                CardBenefitSelection selection = selector.select(
                        card.candidates(), states.get(card.cardId()).toCardState(), payment.request());
                if (beats(selection, card, winningSelection, winner)) {
                    winner = card;
                    winningSelection = selection;
                }
            }
            // 아무 카드도 혜택을 못 줘도 결제는 어느 한 장으로 한다 — 스탬프가 그 카드에만 찍힌다
            total += states.get(winner.cardId()).apply(winningSelection, winner.candidates());
        }

        List<PortfolioSimulation.CardShare> shares = new ArrayList<>();
        states.forEach((cardId, state) -> shares.add(new PortfolioSimulation.CardShare(
                cardId, state.totalBenefitAmount(), state.appliedPaymentCount(), state.breakdowns())));
        shares.sort(Comparator
                .comparingLong(PortfolioSimulation.CardShare::benefitAmount).reversed()
                .thenComparingLong(PortfolioSimulation.CardShare::cardId));
        return new PortfolioSimulation(total, shares);
    }

    /** 혜택액이 크면 이긴다. 같으면 cardId가 작은 쪽 — 결제 화면의 동점 규칙과 같다(재현성) */
    private boolean beats(CardBenefitSelection selection, SimulatedCard card,
                          CardBenefitSelection best, SimulatedCard bestCard) {
        if (bestCard == null) {
            return true;
        }
        if (selection.benefitAmount() != best.benefitAmount()) {
            return selection.benefitAmount() > best.benefitAmount();
        }
        return card.cardId() < bestCard.cardId();
    }
}
