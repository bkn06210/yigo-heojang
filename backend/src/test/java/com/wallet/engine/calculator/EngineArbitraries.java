package com.wallet.engine.calculator;

import com.wallet.engine.calculator.EngineSpecs.AmountLimits;
import com.wallet.engine.calculator.EngineSpecs.BenefitSpec;
import com.wallet.engine.calculator.EngineSpecs.CardSpec;
import com.wallet.engine.calculator.EngineSpecs.ContextSpec;
import com.wallet.engine.calculator.EngineSpecs.CountLimits;
import com.wallet.engine.calculator.EngineSpecs.PaymentSpec;
import com.wallet.engine.calculator.EngineSpecs.Place;
import com.wallet.engine.calculator.EngineSpecs.RuleSpec;
import com.wallet.engine.calculator.EngineSpecs.StatusSpec;
import com.wallet.engine.calculator.EngineSpecs.TargetSpec;
import com.wallet.engine.calculator.EngineSpecs.UsedAmounts;
import com.wallet.engine.calculator.EngineSpecs.UsedCounts;
import com.wallet.engine.model.BenefitKind;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.PerformancePeriod;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.Tuple;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 속성 테스트의 입력 생성기.
 *
 * <b>도메인에 있을 수 있는 입력만 만든다.</b> 음수 한도나 100%를 넘는 요율처럼 있을 수 없는 값으로
 * 실패가 나면 엔진의 결함인지 생성기의 결함인지 가릴 수 없다. 값의 범위와 조합은 카드 약관에서
 * 뽑은 혜택 데이터의 실제 분포를 따랐다 — 대상금액 상한은 정률에만 붙고, 묶음 안의 혜택은
 * 월 한도를 공유하며, 스탬프형은 횟수 묶음 하나에 여러 혜택이 들어간다.
 *
 * 대신 약관에 드문 경계(한도 0, NULL 한도, 건당 최소금액 미달, 실적 미달)는 실제보다 훨씬 자주
 * 섞는다. 사람이 쓴 예시 테스트가 빠뜨리기 쉬운 곳이 거기다.
 */
final class EngineArbitraries {

    static final String CARD = "CARD";
    static final String SIMPLE_PAY = "SIMPLE_PAY";

    private static final String OPTION_GROUP = "OPTION";

    /** 스탬프형(COUNT_STEP) 혜택을 섞을지. 탐욕 선택의 손실을 스탬프 유무로 나눠 재기 위해 갈라 둔다 */
    enum StampMode {
        WITHOUT_STAMP,
        WITH_STAMP
    }

    private EngineArbitraries() {
    }

    // ------------------------------------------------------------------ 값

    /** 결제금액. 천원 단위가 대부분이고, 원 미만 절사를 건드리는 자투리 금액을 섞는다 */
    static Arbitrary<Long> paymentAmounts() {
        return Arbitraries.frequencyOf(
                Tuple.of(6, Arbitraries.longs().between(1L, 60L).map(thousands -> thousands * 1_000L)),
                Tuple.of(3, Arbitraries.longs().between(1L, 300_000L)),
                Tuple.of(1, Arbitraries.longs().between(1L, 999L)));
    }

    static Arbitrary<String> paymentTypes() {
        return Arbitraries.frequency(Tuple.of(6, CARD), Tuple.of(3, SIMPLE_PAY), Tuple.of(1, (String) null));
    }

    /** 금액 한도 값. 0(혜택 없음)을 일부러 섞는다 */
    private static Arbitrary<Long> amountLimitValues() {
        return Arbitraries.frequencyOf(
                Tuple.of(1, Arbitraries.just(0L)),
                Tuple.of(9, Arbitraries.longs().between(1L, 60L).map(units -> units * 500L)));
    }

    private static Arbitrary<Integer> countLimitValues() {
        return Arbitraries.frequencyOf(
                Tuple.of(1, Arbitraries.just(0)),
                Tuple.of(9, Arbitraries.integers().between(1, 6)));
    }

    /** null = 제약 없음. 값이 있을 확률을 축마다 달리 준다(월 한도가 가장 흔하다) */
    private static <T> Arbitrary<T> sometimes(Arbitrary<T> values, double presentProbability) {
        return values.injectNull(1.0 - presentProbability);
    }

    static Arbitrary<AmountLimits> amountLimits() {
        return Combinators.combine(
                sometimes(amountLimitValues(), 0.15),
                sometimes(amountLimitValues(), 0.45),
                sometimes(amountLimitValues(), 0.10),
                sometimes(amountLimitValues(), 0.12)).as(AmountLimits::new);
    }

    static Arbitrary<CountLimits> countLimits() {
        return Combinators.combine(
                sometimes(countLimitValues(), 0.12),
                sometimes(countLimitValues(), 0.25),
                sometimes(countLimitValues(), 0.08),
                sometimes(countLimitValues(), 0.10)).as(CountLimits::new);
    }

    /** 요율(%). 소수 둘째 자리까지, 0%와 100%를 포함한다 */
    private static Arbitrary<BigDecimal> rates() {
        return Arbitraries.frequencyOf(
                Tuple.of(6, Arbitraries.of("10.00", "1.00", "5.00", "0.50", "1.20", "20.00", "30.00", "50.00")
                        .map(BigDecimal::new)),
                Tuple.of(3, Arbitraries.integers().between(0, 10_000)
                        .map(hundredths -> BigDecimal.valueOf(hundredths, 2))),
                Tuple.of(1, Arbitraries.of("0.00", "100.00").map(BigDecimal::new)));
    }

    private static Arbitrary<BigDecimal> fixedAmounts() {
        return Arbitraries.longs().between(0L, 200L).map(hundreds -> BigDecimal.valueOf(hundreds * 100L));
    }

    private static Arbitrary<BigDecimal> stampAmounts() {
        return Arbitraries.longs().between(1L, 50L).map(hundreds -> BigDecimal.valueOf(hundreds * 100L));
    }

    private static Arbitrary<BigDecimal> valuesOf(CalcMethod method) {
        return switch (method) {
            case RATE -> rates();
            case FIXED -> fixedAmounts();
            case COUNT_STEP -> stampAmounts();
        };
    }

    private static Arbitrary<BenefitKind> kinds() {
        // 금액을 내지 않는 종류(증정·사후정산·무이자할부)는 조금만 섞는다. 많으면 대부분의 입력이
        // 첫 관문에서 미적용으로 끝나 한도 규칙까지 가지 못한다
        return Arbitraries.frequency(
                Tuple.of(60, BenefitKind.DISCOUNT),
                Tuple.of(30, BenefitKind.POINT),
                Tuple.of(4, BenefitKind.SPECIAL_PRICE),
                Tuple.of(2, BenefitKind.GIFT),
                Tuple.of(2, BenefitKind.RETROACTIVE),
                Tuple.of(2, BenefitKind.INSTALLMENT_FREE));
    }

    /** 건당 최소금액. 결제금액 분포(대부분 1천~6만원)와 겹치게 두어 통과와 미달이 둘 다 나오게 한다 */
    private static Arbitrary<Long> minTxnAmounts() {
        return sometimes(Arbitraries.frequencyOf(
                Tuple.of(3, Arbitraries.longs().between(1L, 10L).map(thousands -> thousands * 1_000L)),
                Tuple.of(1, Arbitraries.longs().between(11L, 30L).map(thousands -> thousands * 1_000L))), 0.30);
    }

    private static Arbitrary<Long> maxBenefitPerTxnValues() {
        return sometimes(Arbitraries.frequencyOf(
                Tuple.of(1, Arbitraries.just(0L)),
                Tuple.of(19, Arbitraries.longs().between(1L, 100L).map(hundreds -> hundreds * 100L))), 0.30);
    }

    private static Arbitrary<String> requiredPaymentTypes() {
        return Arbitraries.frequency(Tuple.of(9, (String) null), Tuple.of(1, SIMPLE_PAY));
    }

    // ------------------------------------------------------------------ 혜택 규칙

    static Arbitrary<RuleSpec> rules(StampMode stampMode) {
        Arbitrary<CalcMethod> methods = stampMode == StampMode.WITHOUT_STAMP
                ? Arbitraries.frequency(Tuple.of(6, CalcMethod.RATE), Tuple.of(3, CalcMethod.FIXED))
                : Arbitraries.frequency(Tuple.of(5, CalcMethod.RATE), Tuple.of(3, CalcMethod.FIXED),
                        Tuple.of(3, CalcMethod.COUNT_STEP));
        return methods.flatMap(EngineArbitraries::rulesOf);
    }

    /** 계산 방식 하나로 고정한 규칙. 대상금액 상한은 정률에만 붙인다 — 정액에는 율을 곱할 금액이 없다 */
    static Arbitrary<RuleSpec> rulesOf(CalcMethod method) {
        Arbitrary<Integer> stepCounts = method == CalcMethod.COUNT_STEP
                ? Arbitraries.integers().between(1, 6)
                : Arbitraries.just(null);
        Arbitrary<Long> maxEligibleAmounts = method == CalcMethod.RATE
                ? sometimes(Arbitraries.longs().between(1L, 50L).map(thousands -> thousands * 1_000L), 0.20)
                : Arbitraries.just(null);

        Arbitrary<RuleSpec> conditions = Combinators.combine(
                kinds(), valuesOf(method), stepCounts, Arbitraries.of(false, true), requiredPaymentTypes(),
                minTxnAmounts(), maxEligibleAmounts, maxBenefitPerTxnValues()
        ).as((kind, value, stepCount, requirePerformance, requirePaymentType,
              minTxnAmount, maxEligibleAmount, maxBenefitPerTxn) -> new RuleSpec(
                0L, kind, method, value, stepCount, requirePerformance, requirePaymentType,
                minTxnAmount, maxEligibleAmount, maxBenefitPerTxn,
                AmountLimits.none(), CountLimits.none(), false));

        return Combinators.combine(conditions, amountLimits(), countLimits(), Arbitraries.of(false, true))
                .as((rule, amountLimits, countLimits, useSharedLimit) -> rule
                        .withAmountLimits(amountLimits)
                        .withCountLimits(countLimits)
                        .withUseSharedLimit(useSharedLimit));
    }

    // ------------------------------------------------------------------ 계산 컨텍스트

    private static Arbitrary<Long> usedAmountParts() {
        return Arbitraries.frequencyOf(
                Tuple.of(5, Arbitraries.just(0L)),
                Tuple.of(5, Arbitraries.longs().between(0L, 40L).map(units -> units * 500L)));
    }

    private static Arbitrary<Integer> usedCountParts() {
        return Arbitraries.frequencyOf(
                Tuple.of(5, Arbitraries.just(0)),
                Tuple.of(5, Arbitraries.integers().between(0, 6)));
    }

    /** 넓은 기간의 소진은 좁은 기간의 소진을 포함한다. 그래서 차이만 뽑아 쌓아 올린다 */
    static Arbitrary<UsedAmounts> usedAmounts() {
        return Combinators.combine(usedAmountParts(), usedAmountParts(), usedAmountParts(), usedAmountParts())
                .as((daily, restOfMonth, restOfQuarter, restOfYear) -> new UsedAmounts(
                        daily,
                        daily + restOfMonth,
                        daily + restOfMonth + restOfQuarter,
                        daily + restOfMonth + restOfQuarter + restOfYear));
    }

    static Arbitrary<UsedCounts> usedCounts() {
        return Combinators.combine(usedCountParts(), usedCountParts(), usedCountParts(), usedCountParts())
                .as((daily, restOfMonth, restOfQuarter, restOfYear) -> new UsedCounts(
                        daily,
                        daily + restOfMonth,
                        daily + restOfMonth + restOfQuarter,
                        daily + restOfMonth + restOfQuarter + restOfYear));
    }

    private static Arbitrary<Long> sharedMonthlyLimits() {
        return sometimes(Arbitraries.frequencyOf(
                Tuple.of(1, Arbitraries.just(0L)),
                Tuple.of(9, Arbitraries.longs().between(1L, 60L).map(thousands -> thousands * 1_000L))), 0.40);
    }

    static Arbitrary<ContextSpec> contexts() {
        // 포인트 사용분은 지금 항상 0이지만 계산식에 자리가 있으므로 드물게 넣는다
        Arbitrary<Integer> pointPercents = Arbitraries.frequencyOf(
                Tuple.of(9, Arbitraries.just(0)),
                Tuple.of(1, Arbitraries.integers().between(1, 90)));
        return Combinators.combine(
                paymentAmounts(), pointPercents, paymentTypes(),
                Arbitraries.frequency(Tuple.of(17, true), Tuple.of(3, false)), Arbitraries.of(false, true),
                usedAmounts(), usedCounts(), sharedMonthlyLimits()
        ).flatAs((paymentAmount, pointPercent, paymentType, performanceMet, amountEstimated,
                  usedAmounts, usedCounts, sharedMonthlyLimit) -> usedAmountParts().map(sharedLimitUsed ->
                new ContextSpec(paymentAmount, paymentAmount * pointPercent / 100L, paymentType, performanceMet,
                        amountEstimated, usedAmounts, usedCounts, sharedMonthlyLimit, sharedLimitUsed)));
    }

    // ------------------------------------------------------------------ 카드

    static Arbitrary<TargetSpec> targets() {
        return Arbitraries.frequency(
                Tuple.of(2, TargetSpec.all()),
                Tuple.of(1, TargetSpec.category(1L)),
                Tuple.of(1, TargetSpec.category(2L)),
                Tuple.of(2, TargetSpec.category(102L)),
                Tuple.of(1, TargetSpec.category(101L)),
                Tuple.of(1, TargetSpec.category(201L)),
                Tuple.of(1, TargetSpec.category(204L)),
                Tuple.of(2, TargetSpec.merchant(10L)),
                Tuple.of(1, TargetSpec.merchant(11L)),
                Tuple.of(1, TargetSpec.merchant(20L)));
    }

    private static Arbitrary<List<TargetSpec>> distinctTargets(int minSize, int maxSize) {
        return targets().list().ofMinSize(minSize).ofMaxSize(maxSize).uniqueElements();
    }

    /**
     * 한 결제가 둘 이상에 해당하지 않는 대상들. 스탬프 묶음에 쓴다.
     *
     * 스탬프 묶음의 대상이 겹치면(전 가맹점 + 카페, 카페 + 스타벅스) 결제 한 건이 진행 횟수를 두 번
     * 올린다. 엔진이 아직 다루지 않는 경우이고 약관 데이터에도 없는 모양이라 생성하지 않는다.
     */
    private static Arbitrary<List<TargetSpec>> separateTargets(int minSize, int maxSize) {
        return distinctTargets(minSize, maxSize).filter(EngineArbitraries::noneOverlap);
    }

    private static boolean noneOverlap(List<TargetSpec> targets) {
        for (int first = 0; first < targets.size(); first++) {
            for (int second = first + 1; second < targets.size(); second++) {
                if (targets.get(first).overlaps(targets.get(second))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 묶음 없는 혜택 하나 */
    private static Arbitrary<List<BenefitSpec>> standaloneBlocks(StampMode stampMode) {
        return Combinators.combine(rules(stampMode), targets())
                .as((rule, target) -> List.of(new BenefitSpec(rule, target, null, null, null, null, null)));
    }

    /**
     * 금액 묶음 — 대상만 다른 혜택들이 한도를 나눠 쓴다("통신·공과금·마트 각 10%, 합쳐서 월 5천원").
     * 혜택값은 묶음 안에서도 갈릴 수 있다("택시·커피 5%, 영화관 30%, 합쳐서 월 5천원").
     */
    private static Arbitrary<List<BenefitSpec>> limitGroupBlocks(StampMode stampMode) {
        return rules(stampMode).flatMap(base -> Combinators.combine(
                base.method() == CalcMethod.COUNT_STEP ? separateTargets(2, 3) : distinctTargets(2, 3),
                valuesOf(base.method()).injectNull(0.7).list().ofSize(3)
        ).as((targets, memberValues) -> {
            List<BenefitSpec> members = new ArrayList<>();
            for (int index = 0; index < targets.size(); index++) {
                BigDecimal value = memberValues.get(index) == null ? base.value() : memberValues.get(index);
                RuleSpec rule = new RuleSpec(0L, base.kind(), base.method(), value, base.stepCount(),
                        base.requirePerformance(), base.requirePaymentType(), base.minTxnAmount(),
                        base.maxEligibleAmount(), base.maxBenefitPerTxn(), base.amountLimits(),
                        base.countLimits(), base.useSharedLimit());
                members.add(new BenefitSpec(rule, targets.get(index), "LIMIT", null, null, null, null));
            }
            return members;
        }));
    }

    /**
     * 금액 묶음 안에 범위가 더 좁은 한도가 낀 경우.
     *
     * "택시·커피·영화관 합쳐 월 5천원"에 "영화관끼리만 연 4회(연 2만원)"가 겹치는 약관이 실제로 있다.
     * 월 한도는 묶음 전체가, 연 한도와 횟수는 영화관 혜택들만 갖는다. 앞쪽 혜택들이 좁은 범위다.
     */
    private static Arbitrary<List<BenefitSpec>> narrowerScopeBlocks() {
        Arbitrary<RuleSpec> bases = Arbitraries.of(CalcMethod.RATE, CalcMethod.FIXED)
                .flatMap(EngineArbitraries::rulesOf);
        return Combinators.combine(
                bases, distinctTargets(3, 3), amountLimitValues(), sometimes(amountLimitValues(), 0.7),
                sometimes(countLimitValues(), 0.7), Arbitraries.of(false, true)
        ).as((base, targets, sharedMonthlyLimit, narrowYearlyLimit, narrowYearlyCountLimit, narrowIsPair) -> {
            int narrowSize = narrowIsPair ? 2 : 1;
            List<BenefitSpec> members = new ArrayList<>();
            for (int index = 0; index < targets.size(); index++) {
                boolean narrow = index < narrowSize;
                RuleSpec rule = base
                        .withAmountLimits(new AmountLimits(
                                null, sharedMonthlyLimit, null, narrow ? narrowYearlyLimit : null))
                        .withCountLimits(new CountLimits(
                                null, null, null, narrow ? narrowYearlyCountLimit : null));
                members.add(new BenefitSpec(rule, targets.get(index), "LIMIT",
                        narrow ? "NARROW" : null, null, null, null));
            }
            return members;
        });
    }

    /**
     * 스탬프 묶음 — 여러 혜택이 진행 횟수를 함께 센다("편의점·커피 이용 5회마다 2천 포인트").
     * 금액 묶음은 없고 횟수 묶음만 있다. 혜택값과 건당 최소금액은 혜택마다 다를 수 있다.
     */
    private static Arbitrary<List<BenefitSpec>> stampGroupBlocks() {
        return Combinators.combine(
                Arbitraries.integers().between(2, 5), separateTargets(2, 3),
                stampAmounts().list().ofSize(3), minTxnAmounts().list().ofSize(3),
                sometimes(countLimitValues(), 0.5), sometimes(countLimitValues(), 0.3), Arbitraries.of(false, true)
        ).as((stepCount, targets, values, minTxnAmounts, monthlyCountLimit, dailyCountLimit,
              requirePerformance) -> {
            List<BenefitSpec> members = new ArrayList<>();
            for (int index = 0; index < targets.size(); index++) {
                RuleSpec rule = new RuleSpec(0L, BenefitKind.POINT, CalcMethod.COUNT_STEP, values.get(index),
                        stepCount, requirePerformance, null, minTxnAmounts.get(index), null, null,
                        AmountLimits.none(), new CountLimits(dailyCountLimit, monthlyCountLimit, null, null),
                        false);
                members.add(new BenefitSpec(rule, targets.get(index), null, "STAMP", null, null, null));
            }
            return members;
        });
    }

    private static Arbitrary<List<BenefitSpec>> benefitBlocks(StampMode stampMode) {
        if (stampMode == StampMode.WITHOUT_STAMP) {
            return Arbitraries.frequencyOf(
                    Tuple.of(6, standaloneBlocks(stampMode)),
                    Tuple.of(3, limitGroupBlocks(stampMode)),
                    Tuple.of(1, narrowerScopeBlocks()));
        }
        return Arbitraries.frequencyOf(
                Tuple.of(6, standaloneBlocks(stampMode)),
                Tuple.of(3, limitGroupBlocks(stampMode)),
                Tuple.of(1, narrowerScopeBlocks()),
                Tuple.of(2, stampGroupBlocks()));
    }

    static Arbitrary<StatusSpec> statuses() {
        return Combinators.combine(
                Arbitraries.frequency(Tuple.of(17, 300_000L), Tuple.of(3, 0L)), sharedMonthlyLimits()
        ).as(StatusSpec::new);
    }

    /**
     * 카드 한 장. 혜택 id는 cardId × 100 + 순번이라 카드끼리 겹치지 않는다.
     *
     * @param maxBlocks 혜택 덩어리 수 상한. 덩어리 하나가 혜택 1~3개다
     */
    static Arbitrary<CardSpec> cards(long cardId, StampMode stampMode, int maxBlocks) {
        // 선택형 혜택: 덩어리 단위로 선택지를 붙이고, 카드는 그달에 고른 선택지 하나를 든다
        Arbitrary<String> optionKeys = Arbitraries.frequency(
                Tuple.of(8, (String) null), Tuple.of(1, "A"), Tuple.of(1, "B"));
        Arbitrary<PerformancePeriod> periods = Arbitraries.frequency(
                Tuple.of(9, PerformancePeriod.MONTH), Tuple.of(1, PerformancePeriod.QUARTER));
        Arbitrary<List<BenefitSpec>> decoratedBlocks = Combinators.combine(
                benefitBlocks(stampMode), optionKeys, periods
        ).as((block, optionKey, period) -> {
            List<BenefitSpec> decorated = new ArrayList<>();
            for (BenefitSpec benefit : block) {
                decorated.add(new BenefitSpec(benefit.rule(), benefit.target(), benefit.limitGroupCode(),
                        benefit.countGroupCode(), optionKey == null ? null : OPTION_GROUP, optionKey, period));
            }
            return decorated;
        });
        Arbitrary<Map<String, String>> selections = Arbitraries.frequency(
                Tuple.of(5, Map.of(OPTION_GROUP, "A")),
                Tuple.of(2, Map.of(OPTION_GROUP, "B")),
                Tuple.of(3, Map.<String, String>of()));

        return Combinators.combine(
                decoratedBlocks.list().ofMinSize(1).ofMaxSize(maxBlocks),
                statuses(), statuses().injectNull(0.7), selections
        ).as((blocks, monthStatus, quarterStatus, selectedOptionKeys) ->
                assembleCard(cardId, blocks, monthStatus, quarterStatus, selectedOptionKeys));
    }

    /** 덩어리마다 묶음 코드에 순번을 붙여 다른 덩어리와 섞이지 않게 하고, 혜택 id를 매긴다 */
    private static CardSpec assembleCard(long cardId, List<List<BenefitSpec>> blocks, StatusSpec monthStatus,
                                         StatusSpec quarterStatus, Map<String, String> selectedOptionKeys) {
        List<BenefitSpec> benefits = new ArrayList<>();
        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            for (BenefitSpec benefit : blocks.get(blockIndex)) {
                long benefitId = cardId * 100L + benefits.size() + 1L;
                benefits.add(new BenefitSpec(
                        benefit.rule().withBenefitId(benefitId),
                        benefit.target(),
                        suffixed(benefit.limitGroupCode(), blockIndex),
                        suffixed(benefit.countGroupCode(), blockIndex),
                        benefit.optionGroupCode(),
                        benefit.optionKey(),
                        benefit.performancePeriod()));
            }
        }
        return new CardSpec(cardId, List.copyOf(benefits), monthStatus, quarterStatus, selectedOptionKeys);
    }

    private static String suffixed(String groupCode, int blockIndex) {
        return groupCode == null ? null : groupCode + "_" + blockIndex;
    }

    /** 카드 여러 장. cardId는 1부터 차례로 매긴다 */
    static Arbitrary<List<CardSpec>> cardLists(int minCards, int maxCards, StampMode stampMode, int maxBlocks) {
        return Arbitraries.integers().between(minCards, maxCards).flatMap(cardCount -> {
            Arbitrary<List<CardSpec>> cards = Arbitraries.just(List.of());
            for (int index = 0; index < cardCount; index++) {
                long cardId = index + 1L;
                cards = Combinators.combine(cards, cards(cardId, stampMode, maxBlocks)).as((soFar, card) -> {
                    List<CardSpec> extended = new ArrayList<>(soFar);
                    extended.add(card);
                    return List.copyOf(extended);
                });
            }
            return cards;
        });
    }

    // ------------------------------------------------------------------ 결제

    static Arbitrary<PaymentSpec> payments() {
        Arbitrary<Place> places = Arbitraries.frequency(
                Tuple.of(4, Place.STARBUCKS), Tuple.of(2, Place.EDIYA), Tuple.of(2, Place.RESTAURANT),
                Tuple.of(2, Place.GS25), Tuple.of(2, Place.ONLINE_MALL), Tuple.of(1, Place.UNSPECIFIED));
        // 날짜를 며칠에 몰아야 같은 날 결제가 생겨 일 한도·일 횟수가 걸린다
        return Combinators.combine(Arbitraries.integers().between(1, 4), places, paymentAmounts(), paymentTypes())
                .as(PaymentSpec::new);
    }

    static Arbitrary<List<PaymentSpec>> paymentLists(int minSize, int maxSize) {
        return payments().list().ofMinSize(minSize).ofMaxSize(maxSize);
    }
}
