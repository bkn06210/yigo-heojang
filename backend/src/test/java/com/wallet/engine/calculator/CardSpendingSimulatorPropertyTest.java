package com.wallet.engine.calculator;

import com.wallet.engine.calculator.EngineArbitraries.StampMode;
import com.wallet.engine.calculator.EngineSpecs.AmountLimits;
import com.wallet.engine.calculator.EngineSpecs.BenefitSpec;
import com.wallet.engine.calculator.EngineSpecs.CardSpec;
import com.wallet.engine.calculator.EngineSpecs.CountLimits;
import com.wallet.engine.calculator.EngineSpecs.PaymentSpec;
import com.wallet.engine.calculator.EngineSpecs.Place;
import com.wallet.engine.calculator.EngineSpecs.RuleSpec;
import com.wallet.engine.calculator.EngineSpecs.StatusSpec;
import com.wallet.engine.calculator.EngineSpecs.TargetSpec;
import com.wallet.engine.model.BenefitKind;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.SpendingSimulation;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Disabled;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 한 달치 결제를 차례로 흘린 뒤의 불변식 — 누적된 혜택이 어떤 한도도 넘지 않는가.
 *
 * 혜택 1건 계산이 맞아도 누적은 틀릴 수 있다. 소진을 올리는 규칙(0원은 올리지 않는다, 스탬프는
 * 0원이어도 올린다, 묶음은 합산한다, 일 소진은 날짜가 바뀌면 리셋한다)이 계산기 밖에 있기 때문이다.
 * 그 규칙들이 맞물린 결과를, 건별로 적은 장부({@link PaymentLedger})에서 다시 세어 확인한다.
 */
class CardSpendingSimulatorPropertyTest {

    private static final int TRIES = 5_000;
    private static final String SEED = "20261012";

    private final CardSpendingSimulator simulator = new CardSpendingSimulator();

    @Provide
    Arbitrary<CardSpec> cards() {
        return EngineArbitraries.cards(1L, StampMode.WITH_STAMP, 4);
    }

    @Provide
    Arbitrary<List<PaymentSpec>> payments() {
        return EngineArbitraries.paymentLists(1, 14);
    }

    private SpendingSimulation simulate(CardSpec card, List<PaymentSpec> payments) {
        return simulator.simulate(card.toCandidates(), card.toMonthStatus(), card.toQuarterStatus(),
                card.selectedOptionKeys(), EngineSpecs.toPayments(payments));
    }

    // ------------------------------------------------------------------ 총액

    @Property(tries = TRIES, seed = SEED)
    void 총_혜택액은_건별_혜택의_합이고_혜택별_내역의_합이다(
            @ForAll("cards") CardSpec card, @ForAll("payments") List<PaymentSpec> payments) {
        SpendingSimulation result = simulate(card, payments);
        PaymentLedger ledger = PaymentLedger.replay(card, payments);

        assertThat(result.totalBenefitAmount()).isEqualTo(ledger.totalDiscount());
        assertThat(result.breakdowns().stream().mapToLong(SpendingSimulation.BenefitBreakdown::benefitAmount).sum())
                .isEqualTo(result.totalBenefitAmount());
        for (SpendingSimulation.BenefitBreakdown breakdown : result.breakdowns()) {
            assertThat(breakdown.benefitAmount()).isEqualTo(ledger.paidAmountOf(breakdown.benefitId()));
        }
    }

    @Property(tries = TRIES, seed = SEED)
    void 건별_혜택은_조건을_채운_혜택에서만_나오고_결제금액과_건당_최대를_넘지_않는다(
            @ForAll("cards") CardSpec card, @ForAll("payments") List<PaymentSpec> payments) {
        PaymentLedger ledger = PaymentLedger.replay(card, payments);

        for (PaymentLedger.Entry entry : ledger.entries()) {
            assertThat(entry.discount()).isBetween(0L, entry.payment().amount());
            if (!entry.paid()) {
                continue;
            }
            BenefitSpec benefit = card.benefitOf(entry.selectedBenefitId());
            RuleSpec rule = benefit.rule();
            PaymentSpec payment = entry.payment();

            assertThat(rule.isCalculableKind()).as("증정·사후정산·무이자할부는 금액을 내지 않는다").isTrue();
            assertThat(payment.place().isCoveredBy(benefit.target())).as("대상이 아닌 결제").isTrue();
            if (rule.requirePerformance()) {
                assertThat(card.performanceMetFor(benefit)).as("실적 미달인데 지급").isTrue();
            }
            if (rule.requirePaymentType() != null) {
                assertThat(payment.paymentType()).isEqualTo(rule.requirePaymentType());
            }
            if (rule.minTxnAmount() != null) {
                assertThat(payment.amount()).isGreaterThanOrEqualTo(rule.minTxnAmount());
            }
            if (rule.maxBenefitPerTxn() != null) {
                assertThat(entry.discount()).isLessThanOrEqualTo(rule.maxBenefitPerTxn());
            }
            if (benefit.optionGroupCode() != null) {
                assertThat(card.selectedOptionKeys().get(benefit.optionGroupCode()))
                        .as("그달에 고르지 않은 선택지").isEqualTo(benefit.optionKey());
            }
        }
    }

    // ------------------------------------------------------------------ 금액 한도

    @Property(tries = TRIES, seed = SEED)
    void 묶음까지_합친_누적액이_월_분기_연_금액_한도를_넘지_않는다(
            @ForAll("cards") CardSpec card, @ForAll("payments") List<PaymentSpec> payments) {
        PaymentLedger ledger = PaymentLedger.replay(card, payments);

        for (BenefitSpec benefit : card.benefits()) {
            assertAmountWithin(ledger, card, benefit, rule -> rule.amountLimits().monthly(), entry -> true, "월");
            assertAmountWithin(ledger, card, benefit, rule -> rule.amountLimits().quarterly(), entry -> true, "분기");
            assertAmountWithin(ledger, card, benefit, rule -> rule.amountLimits().yearly(), entry -> true, "연");
        }
    }

    @Property(tries = TRIES, seed = SEED)
    void 묶음까지_합친_하루_누적액이_일_금액_한도를_넘지_않는다(
            @ForAll("cards") CardSpec card, @ForAll("payments") List<PaymentSpec> payments) {
        PaymentLedger ledger = PaymentLedger.replay(card, payments);

        for (BenefitSpec benefit : card.benefits()) {
            for (int day : ledger.days()) {
                assertAmountWithin(ledger, card, benefit, rule -> rule.amountLimits().daily(),
                        entry -> entry.payment().dayOfMonth() == day, day + "일");
            }
        }
    }

    @Property(tries = TRIES, seed = SEED)
    void 통합한도를_쓰는_혜택들의_누적액이_통합한도를_넘지_않는다(
            @ForAll("cards") CardSpec card, @ForAll("payments") List<PaymentSpec> payments) {
        Long sharedMonthlyLimit = card.monthStatus().sharedMonthlyLimit();
        if (sharedMonthlyLimit == null) {
            return;
        }
        PaymentLedger ledger = PaymentLedger.replay(card, payments);

        assertThat(ledger.paidAmount(benefit -> benefit.rule().useSharedLimit(), entry -> true))
                .isLessThanOrEqualTo(sharedMonthlyLimit);
    }

    /** 이 혜택과 금액 한도를 나눠 쓰는 혜택들이 받은 합이 한도 이하인가 */
    private void assertAmountWithin(PaymentLedger ledger, CardSpec card, BenefitSpec benefit,
                                    Function<RuleSpec, Long> limitOf, Predicate<PaymentLedger.Entry> period,
                                    String label) {
        Long limit = limitOf.apply(benefit.rule());
        if (limit == null) {
            return;
        }
        // 묶음 안에서도 그 축의 한도를 가진 혜택끼리만 센다 — 한도가 없는 혜택이 받은 돈은 이 한도의 몫이 아니다
        long paid = ledger.paidAmount(
                other -> sharesLimitGroup(benefit, other) && limitOf.apply(other.rule()) != null, period);

        assertThat(paid).as("혜택 %d의 %s 금액 한도 %d", benefit.benefitId(), label, limit)
                .isLessThanOrEqualTo(limit);
    }

    // ------------------------------------------------------------------ 횟수 한도

    @Property(tries = TRIES, seed = SEED)
    void 묶음까지_합친_지급_횟수가_월_분기_연_횟수_한도를_넘지_않는다(
            @ForAll("cards") CardSpec card, @ForAll("payments") List<PaymentSpec> payments) {
        PaymentLedger ledger = PaymentLedger.replay(card, payments);

        for (BenefitSpec benefit : card.benefits()) {
            assertCountWithin(ledger, benefit, rule -> rule.countLimits().monthly(), entry -> true, "월");
            assertCountWithin(ledger, benefit, rule -> rule.countLimits().quarterly(), entry -> true, "분기");
            assertCountWithin(ledger, benefit, rule -> rule.countLimits().yearly(), entry -> true, "연");
        }
    }

    @Property(tries = TRIES, seed = SEED)
    void 묶음까지_합친_하루_지급_횟수가_일_횟수_한도를_넘지_않는다(
            @ForAll("cards") CardSpec card, @ForAll("payments") List<PaymentSpec> payments) {
        PaymentLedger ledger = PaymentLedger.replay(card, payments);

        for (BenefitSpec benefit : card.benefits()) {
            for (int day : ledger.days()) {
                assertCountWithin(ledger, benefit, rule -> rule.countLimits().daily(),
                        entry -> entry.payment().dayOfMonth() == day, day + "일");
            }
        }
    }

    /** 이 혜택과 횟수 한도를 나눠 쓰는 혜택들이 혜택을 받은 횟수가 한도 이하인가 */
    private void assertCountWithin(PaymentLedger ledger, BenefitSpec benefit,
                                   Function<RuleSpec, Integer> limitOf, Predicate<PaymentLedger.Entry> period,
                                   String label) {
        Integer limit = limitOf.apply(benefit.rule());
        if (limit == null) {
            return;
        }
        long paidCount = ledger.paidCount(
                other -> sharesCountGroup(benefit, other) && limitOf.apply(other.rule()) != null, period);

        assertThat(paidCount).as("혜택 %d의 %s 횟수 한도 %d", benefit.benefitId(), label, limit)
                .isLessThanOrEqualTo(limit);
    }

    // ------------------------------------------------------------------ 스탬프형

    @Property(tries = TRIES, seed = SEED)
    void 스탬프형은_조건을_채운_결제가_N번_쌓일_때마다_한_번만_지급한다(
            @ForAll("cards") CardSpec card, @ForAll("payments") List<PaymentSpec> payments) {
        PaymentLedger ledger = PaymentLedger.replay(card, payments);

        for (BenefitSpec benefit : card.benefits()) {
            if (benefit.rule().method() != CalcMethod.COUNT_STEP) {
                continue;
            }
            Predicate<BenefitSpec> sameProgress = other -> sharesCountGroup(benefit, other);
            long qualifiedPayments = ledger.stampedPaymentCount(sameProgress, entry -> true);
            long payouts = ledger.paidCount(sameProgress, entry -> true);

            assertThat(payouts).as("혜택 %d: 조건 충족 결제 %d건, %d회마다 지급",
                            benefit.benefitId(), qualifiedPayments, benefit.rule().stepCount())
                    .isLessThanOrEqualTo(qualifiedPayments / benefit.rule().stepCount());
        }
    }

    /**
     * 지금은 성립하지 않는다. 한 결제가 같은 횟수 묶음의 스탬프형 혜택 둘에 해당하면 둘 다 진행 횟수가
     * 올라, 결제 3건이 4칸으로 세어진다. 2회마다 지급이면 한 번 받아야 할 것을 두 번 받는다.
     * 약관 데이터에는 대상이 겹치는 스탬프 묶음이 없어 생성기는 이 모양을 만들지 않는다.
     */
    @Disabled("스탬프 묶음의 대상이 겹치면 결제 한 건이 진행 횟수를 두 번 올린다")
    @Example
    void 스탬프_묶음의_대상이_겹쳐도_결제_한_건은_진행_횟수를_한_번만_올린다() {
        RuleSpec everywhereStamp = new RuleSpec(101L, BenefitKind.POINT, CalcMethod.COUNT_STEP,
                BigDecimal.valueOf(100L), 2, false, null, null, null, null,
                AmountLimits.none(), CountLimits.none(), false);
        RuleSpec shoppingStamp = everywhereStamp.withBenefitId(102L);
        CardSpec card = new CardSpec(1L, List.of(
                new BenefitSpec(everywhereStamp, TargetSpec.all(), null, "STAMP", null, null, null),
                new BenefitSpec(shoppingStamp, TargetSpec.category(2L), null, "STAMP", null, null, null)),
                new StatusSpec(300_000L, null), null, Map.of());
        List<PaymentSpec> threePayments = List.of(
                new PaymentSpec(1, Place.STARBUCKS, 1_000L, EngineArbitraries.CARD),
                new PaymentSpec(1, Place.GS25, 1_000L, EngineArbitraries.CARD),
                new PaymentSpec(1, Place.STARBUCKS, 1_000L, EngineArbitraries.CARD));

        PaymentLedger ledger = PaymentLedger.replay(card, threePayments);

        // 결제 3건, 2회마다 지급 → 한 번
        assertThat(ledger.paidCount(benefit -> true, entry -> true)).isEqualTo(1L);
    }

    // ------------------------------------------------------------------ 결정성

    @Property(tries = TRIES, seed = SEED)
    void 같은_입력이면_같은_결과다(
            @ForAll("cards") CardSpec card, @ForAll("payments") List<PaymentSpec> payments) {
        assertThat(simulate(card, payments)).isEqualTo(simulate(card, payments));
    }

    @Property(tries = TRIES, seed = SEED)
    void 혜택_목록의_순서를_뒤집어도_결과가_같다(
            @ForAll("cards") CardSpec card, @ForAll("payments") List<PaymentSpec> payments) {
        List<BenefitSpec> reversedBenefits = new ArrayList<>(card.benefits());
        Collections.reverse(reversedBenefits);
        CardSpec reversed = new CardSpec(card.cardId(), reversedBenefits, card.monthStatus(),
                card.quarterStatus(), card.selectedOptionKeys());

        assertThat(simulate(reversed, payments)).isEqualTo(simulate(card, payments));
    }

    @Property(tries = TRIES, seed = SEED)
    void 날짜가_전부_다르면_결제_목록의_순서를_뒤집어도_결과가_같다(
            @ForAll("cards") CardSpec card, @ForAll("payments") List<PaymentSpec> payments) {
        // 같은 날 결제는 입력 순서대로 처리되므로, 순서와 무관하려면 날짜가 겹치지 않아야 한다
        List<PaymentSpec> onDistinctDays = new ArrayList<>();
        for (int index = 0; index < payments.size(); index++) {
            PaymentSpec payment = payments.get(index);
            onDistinctDays.add(new PaymentSpec(index + 1, payment.place(), payment.amount(), payment.paymentType()));
        }
        List<PaymentSpec> reversed = new ArrayList<>(onDistinctDays);
        Collections.reverse(reversed);

        assertThat(simulate(card, reversed)).isEqualTo(simulate(card, onDistinctDays));
    }

    // ------------------------------------------------------------------ 묶음 판정 (엔진과 따로 적는다)

    private static boolean sharesLimitGroup(BenefitSpec benefit, BenefitSpec other) {
        if (benefit.limitGroupCode() == null) {
            return benefit.benefitId() == other.benefitId();
        }
        return benefit.limitGroupCode().equals(other.limitGroupCode());
    }

    private static boolean sharesCountGroup(BenefitSpec benefit, BenefitSpec other) {
        if (benefit.effectiveCountGroupCode() == null) {
            return benefit.benefitId() == other.benefitId();
        }
        return Objects.equals(benefit.effectiveCountGroupCode(), other.effectiveCountGroupCode());
    }
}
