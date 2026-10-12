package com.wallet.engine.calculator;

import com.wallet.engine.calculator.EngineSpecs.BenefitSpec;
import com.wallet.engine.calculator.EngineSpecs.CardSpec;
import com.wallet.engine.calculator.EngineSpecs.PaymentSpec;
import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.CapType;
import com.wallet.engine.model.CardBenefitSelection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * 카드 한 장에 결제 열을 흘려보내며 <b>건별 결과</b>를 적어 둔 장부.
 *
 * 시뮬레이터는 총액과 혜택별 합계만 돌려준다. 그것만으로는 "일 한도를 넘지 않았다",
 * "5회째에만 지급했다" 같은 규칙을 확인할 수 없어, 같은 순서로 한 건씩 흘리며 따로 적는다.
 * 불변식은 이 장부와 입력 명세만으로 검사한다 — 엔진이 누적해 둔 소진값을 다시 읽지 않는다.
 */
final class PaymentLedger {

    /**
     * 결제 한 건의 결과.
     *
     * @param selectedBenefitId 카드가 고른 혜택. 받을 혜택이 없으면 null (0원이어도 고른 혜택은 남는다)
     * @param discount          실제로 받은 혜택액
     * @param appliedCap        고른 혜택에 기록된 상한
     * @param stampedBenefitIds 이 결제로 진행 횟수가 오른 스탬프형 혜택들
     */
    record Entry(PaymentSpec payment, Long selectedBenefitId, long discount, CapType appliedCap,
                 List<Long> stampedBenefitIds) {

        boolean paid() {
            return discount > 0;
        }
    }

    private final CardSpec card;
    private final List<Entry> entries;

    private PaymentLedger(CardSpec card, List<Entry> entries) {
        this.card = card;
        this.entries = List.copyOf(entries);
    }

    /** 시뮬레이터와 같은 순서(날짜순, 같은 날은 입력 순서)로 흘린다 */
    static PaymentLedger replay(CardSpec card, List<PaymentSpec> payments) {
        CardBenefitSelector selector = new CardBenefitSelector();
        List<BenefitCandidate> candidates = card.toCandidates();
        CardRunState state = new CardRunState(
                card.toMonthStatus(), card.toQuarterStatus(), card.selectedOptionKeys());

        List<PaymentSpec> ordered = new ArrayList<>(payments);
        ordered.sort(Comparator.comparingInt(PaymentSpec::dayOfMonth));

        List<Entry> entries = new ArrayList<>();
        int currentDay = -1;
        for (PaymentSpec payment : ordered) {
            if (payment.dayOfMonth() != currentDay) {
                state.resetDaily();
                currentDay = payment.dayOfMonth();
            }
            CardBenefitSelection selection = selector.select(candidates, state.toCardState(), payment.toRequest());
            long discount = state.apply(selection, candidates);
            entries.add(new Entry(payment, selection.benefitId(), discount, selection.appliedCap(),
                    selection.stampedBenefitIds()));
        }
        return new PaymentLedger(card, entries);
    }

    List<Entry> entries() {
        return entries;
    }

    long totalDiscount() {
        return entries.stream().mapToLong(Entry::discount).sum();
    }

    /** 혜택 하나가 받은 금액 합 */
    long paidAmountOf(long benefitId) {
        return paidAmount(benefit -> benefit.benefitId() == benefitId, entry -> true);
    }

    /** 조건에 맞는 혜택들이 조건에 맞는 결제에서 받은 금액 합 */
    long paidAmount(Predicate<BenefitSpec> benefits, Predicate<Entry> payments) {
        return entries.stream()
                .filter(Entry::paid)
                .filter(payments)
                .filter(entry -> benefits.test(card.benefitOf(entry.selectedBenefitId())))
                .mapToLong(Entry::discount)
                .sum();
    }

    /** 조건에 맞는 혜택들이 조건에 맞는 결제에서 혜택을 받은 횟수 */
    long paidCount(Predicate<BenefitSpec> benefits, Predicate<Entry> payments) {
        return entries.stream()
                .filter(Entry::paid)
                .filter(payments)
                .filter(entry -> benefits.test(card.benefitOf(entry.selectedBenefitId())))
                .count();
    }

    /** 조건에 맞는 스탬프형 혜택 중 하나라도 진행 횟수가 오른 결제 수 */
    long stampedPaymentCount(Predicate<BenefitSpec> benefits, Predicate<Entry> payments) {
        return entries.stream()
                .filter(payments)
                .filter(entry -> entry.stampedBenefitIds().stream()
                        .anyMatch(stampedId -> benefits.test(card.benefitOf(stampedId))))
                .count();
    }

    /** 결제가 있었던 날짜들 */
    List<Integer> days() {
        return entries.stream().map(entry -> entry.payment().dayOfMonth()).distinct().toList();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Entry entry : entries) {
            builder.append("\n      ").append(entry.payment())
                    .append(" → 혜택 ").append(entry.selectedBenefitId())
                    .append(", ").append(entry.discount()).append("원");
            if (entry.appliedCap() != CapType.NONE) {
                builder.append(" (").append(entry.appliedCap()).append(")");
            }
            if (!entry.stampedBenefitIds().isEmpty()) {
                builder.append(" 스탬프 ").append(entry.stampedBenefitIds());
            }
        }
        return builder.toString();
    }
}
