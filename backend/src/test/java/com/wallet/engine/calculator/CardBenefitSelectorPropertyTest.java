package com.wallet.engine.calculator;

import com.wallet.engine.calculator.EngineSpecs.AmountLimits;
import com.wallet.engine.calculator.EngineSpecs.BenefitSpec;
import com.wallet.engine.calculator.EngineSpecs.CountLimits;
import com.wallet.engine.calculator.EngineSpecs.Place;
import com.wallet.engine.calculator.EngineSpecs.RuleSpec;
import com.wallet.engine.calculator.EngineSpecs.TargetSpec;
import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.BenefitKind;
import com.wallet.engine.model.BenefitUsage;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.CardBenefitSelection;
import com.wallet.engine.model.CardState;
import com.wallet.engine.model.PaymentRequest;
import com.wallet.engine.model.PerformanceStatus;
import net.jqwik.api.Disabled;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 금액 묶음 안에 범위가 더 좁은 한도가 낀 카드의 불변식.
 *
 * "택시·커피·영화관 합쳐 월 5천원"에 "영화관끼리만 연 4회(연 2만원)"가 겹치는 약관이 있다.
 * 월 한도는 묶음 전체의 것이고 연 한도는 영화관만의 것이다. 그렇다면 택시를 아무리 타도
 * 영화관의 연 한도는 줄지 않아야 한다 — 택시 혜택에는 그 연 한도가 걸려 있지 않다.
 *
 * 지난달까지의 소진으로 검사한다. 이번 달 소진은 월 한도를 정당하게 함께 깎으므로 섞으면 구분이 안 된다.
 */
class CardBenefitSelectorPropertyTest {

    private static final int TRIES = 10_000;
    private static final String SEED = "20261012";
    private static final long NARROW_BENEFIT_ID = 1L;
    private static final long WIDE_BENEFIT_ID = 2L;
    private static final String LIMIT_GROUP = "PLAY_MONTHLY";
    private static final String NARROW_COUNT_GROUP = "MOVIE_YEARLY";

    private final CardBenefitSelector selector = new CardBenefitSelector();

    /**
     * 지금은 성립하지 않는다. 묶음의 소진액을 기간 축을 가리지 않고 묶음 전체로 합산하기 때문에,
     * 연 한도가 없는 택시 혜택이 받은 금액까지 영화관의 연 한도에서 빠진다. 월 5천원씩 넉 달을 택시로
     * 채우면 영화관은 한 번도 안 갔는데 연 2만원이 다 찬 것으로 계산돼 그해 영화관 할인이 0원이 된다.
     *
     * 횟수는 횟수 묶음(count_group_code)을 따로 둘 수 있어 아래 속성이 성립한다. 금액에는 그 수단이 없다.
     */
    @Disabled("금액 묶음 안에서 범위가 더 좁은 기간 한도를 표현할 수 없다 — 묶음 전체의 소진이 좁은 한도를 깎는다")
    @Property(tries = TRIES, seed = SEED)
    void 좁은_범위의_연_금액_한도는_그_한도가_없는_혜택의_지난달_소진으로_줄지_않는다(
            @ForAll @IntRange(min = 1, max = 50) int ratePercent,
            @ForAll @IntRange(min = 1, max = 20) int monthlyLimitThousands,
            @ForAll @IntRange(min = 1, max = 40) int narrowYearlyLimitThousands,
            @ForAll @IntRange(min = 0, max = 60) int wideUsedEarlierThisYearThousands,
            @ForAll @IntRange(min = 1, max = 60) int paymentThousands) {
        AmountLimits narrowLimits = new AmountLimits(
                null, monthlyLimitThousands * 1_000L, null, narrowYearlyLimitThousands * 1_000L);
        AmountLimits wideLimits = new AmountLimits(null, monthlyLimitThousands * 1_000L, null, null);
        List<BenefitCandidate> candidates = List.of(
                member(NARROW_BENEFIT_ID, Place.STARBUCKS, ratePercent, narrowLimits, CountLimits.none(), null),
                member(WIDE_BENEFIT_ID, Place.GS25, ratePercent, wideLimits, CountLimits.none(), null));
        PaymentRequest narrowPayment =
                PaymentRequest.confirmed(Place.STARBUCKS.toTarget(), paymentThousands * 1_000L, "CARD", false);

        // 넓은 쪽 혜택이 올해 앞선 달들에 받은 금액. 이번 달 소진은 0이라 월 한도는 그대로다
        BenefitUsage wideUsedEarlier = new BenefitUsage(WIDE_BENEFIT_ID, 0L, 0, 0L, 0,
                0L, 0, wideUsedEarlierThisYearThousands * 1_000L, 0);

        CardBenefitSelection fresh = selector.select(candidates, stateWith(List.of()), narrowPayment);
        CardBenefitSelection afterWideUsage =
                selector.select(candidates, stateWith(List.of(wideUsedEarlier)), narrowPayment);

        assertThat(afterWideUsage.benefitAmount()).isEqualTo(fresh.benefitAmount());
    }

    @Property(tries = TRIES, seed = SEED)
    void 좁은_범위의_연_횟수_한도는_횟수_묶음을_따로_두면_다른_혜택의_지난달_횟수로_줄지_않는다(
            @ForAll @IntRange(min = 1, max = 50) int ratePercent,
            @ForAll @IntRange(min = 1, max = 20) int monthlyLimitThousands,
            @ForAll @IntRange(min = 1, max = 6) int narrowYearlyCountLimit,
            @ForAll @IntRange(min = 0, max = 30) int wideCountEarlierThisYear,
            @ForAll @IntRange(min = 1, max = 60) int paymentThousands) {
        AmountLimits sharedLimits = new AmountLimits(null, monthlyLimitThousands * 1_000L, null, null);
        List<BenefitCandidate> candidates = List.of(
                member(NARROW_BENEFIT_ID, Place.STARBUCKS, ratePercent, sharedLimits,
                        new CountLimits(null, null, null, narrowYearlyCountLimit), NARROW_COUNT_GROUP),
                member(WIDE_BENEFIT_ID, Place.GS25, ratePercent, sharedLimits, CountLimits.none(), null));
        PaymentRequest narrowPayment =
                PaymentRequest.confirmed(Place.STARBUCKS.toTarget(), paymentThousands * 1_000L, "CARD", false);

        BenefitUsage wideUsedEarlier = new BenefitUsage(WIDE_BENEFIT_ID, 0L, 0, 0L, 0,
                0L, 0, 0L, wideCountEarlierThisYear);

        CardBenefitSelection fresh = selector.select(candidates, stateWith(List.of()), narrowPayment);
        CardBenefitSelection afterWideUsage =
                selector.select(candidates, stateWith(List.of(wideUsedEarlier)), narrowPayment);

        assertThat(afterWideUsage.benefitAmount()).isEqualTo(fresh.benefitAmount());
    }

    private BenefitCandidate member(long benefitId, Place place, int ratePercent, AmountLimits amountLimits,
                                    CountLimits countLimits, String countGroupCode) {
        RuleSpec rule = new RuleSpec(benefitId, BenefitKind.DISCOUNT, CalcMethod.RATE,
                BigDecimal.valueOf(ratePercent), null, false, null, null, null, null,
                amountLimits, countLimits, false);
        return new BenefitSpec(rule, TargetSpec.merchant(merchantIdOf(place)), LIMIT_GROUP, countGroupCode,
                null, null, null).toCandidate();
    }

    private long merchantIdOf(Place place) {
        return place.toTarget().getMerchantId();
    }

    private CardState stateWith(List<BenefitUsage> usages) {
        return new CardState(new PerformanceStatus(1L, 300_000L, null), 0L, usages);
    }
}
