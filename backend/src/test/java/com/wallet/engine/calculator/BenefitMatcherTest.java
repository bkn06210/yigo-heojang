package com.wallet.engine.calculator;

import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.BenefitExclusion;
import com.wallet.engine.model.BenefitKind;
import com.wallet.engine.model.BenefitRule;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.ExclusionType;
import com.wallet.engine.model.PaymentRequest;
import com.wallet.engine.model.PaymentTarget;
import com.wallet.engine.model.TargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BenefitMatcherTest {

    private static final long CAFE_CATEGORY_ID = 102L;
    private static final long DINING_CATEGORY_ID = 100L;
    private static final long STARBUCKS_MERCHANT_ID = 11L;

    private final BenefitMatcher matcher = new BenefitMatcher();

    private BenefitRule anyRule() {
        return BenefitRule.builder()
                .benefitId(1L)
                .benefitKind(BenefitKind.DISCOUNT)
                .calcMethod(CalcMethod.RATE)
                .benefitValue(new BigDecimal("10.00"))
                .build();
    }

    /** 스타벅스(카페 > 외식) 결제 */
    private PaymentTarget starbucksPayment() {
        return PaymentTarget.builder()
                .merchantId(STARBUCKS_MERCHANT_ID)
                .merchantCode("STARBUCKS")
                .categoryId(CAFE_CATEGORY_ID)
                .categoryCode("CAFE")
                .parentCategoryId(DINING_CATEGORY_ID)
                .parentCategoryCode("DINING")
                .build();
    }

    private PaymentRequest request(PaymentTarget target, String paymentType) {
        return PaymentRequest.estimated(target, 10000L, paymentType);
    }

    @Nested
    @DisplayName("대상 매칭")
    class TargetMatching {

        @Test
        void 전_가맹점_혜택은_항상_매칭된다() {
            BenefitCandidate candidate = BenefitCandidate.builder()
                    .targetType(TargetType.ALL)
                    .rule(anyRule())
                    .build();

            assertThat(matcher.matches(candidate, request(starbucksPayment(), null))).isTrue();
            assertThat(matcher.matches(candidate, request(PaymentTarget.unspecified(), null))).isTrue();
        }

        @Test
        void 가맹점_혜택은_그_가맹점_결제에만_매칭된다() {
            BenefitCandidate candidate = BenefitCandidate.builder()
                    .targetType(TargetType.MERCHANT)
                    .targetMerchantId(STARBUCKS_MERCHANT_ID)
                    .rule(anyRule())
                    .build();

            PaymentTarget other = PaymentTarget.builder().merchantId(99L).merchantCode("CU").build();

            assertThat(matcher.matches(candidate, request(starbucksPayment(), null))).isTrue();
            assertThat(matcher.matches(candidate, request(other, null))).isFalse();
        }

        @Test
        void 가맹점_혜택은_장소_미정_결제에_매칭되지_않는다() {
            BenefitCandidate candidate = BenefitCandidate.builder()
                    .targetType(TargetType.MERCHANT)
                    .targetMerchantId(STARBUCKS_MERCHANT_ID)
                    .rule(anyRule())
                    .build();

            assertThat(matcher.matches(candidate, request(PaymentTarget.unspecified(), null))).isFalse();
        }

        @Test
        void 카테고리_혜택은_같은_카테고리_결제에_매칭된다() {
            BenefitCandidate candidate = BenefitCandidate.builder()
                    .targetType(TargetType.CATEGORY)
                    .targetCategoryId(CAFE_CATEGORY_ID)
                    .rule(anyRule())
                    .build();

            assertThat(matcher.matches(candidate, request(starbucksPayment(), null))).isTrue();
        }

        @Test
        void 대분류_혜택은_하위_중분류_결제에도_매칭된다() {
            // "외식 5%" 혜택이 카페(외식의 하위) 결제에 적용된다
            BenefitCandidate candidate = BenefitCandidate.builder()
                    .targetType(TargetType.CATEGORY)
                    .targetCategoryId(DINING_CATEGORY_ID)
                    .rule(anyRule())
                    .build();

            assertThat(matcher.matches(candidate, request(starbucksPayment(), null))).isTrue();
        }

        @Test
        void 무관한_카테고리_혜택은_매칭되지_않는다() {
            BenefitCandidate candidate = BenefitCandidate.builder()
                    .targetType(TargetType.CATEGORY)
                    .targetCategoryId(301L)
                    .rule(anyRule())
                    .build();

            assertThat(matcher.matches(candidate, request(starbucksPayment(), null))).isFalse();
        }
    }

    @Nested
    @DisplayName("제외 판정")
    class Exclusions {

        private BenefitCandidate diningBenefitExcluding(BenefitExclusion... exclusions) {
            return BenefitCandidate.builder()
                    .targetType(TargetType.CATEGORY)
                    .targetCategoryId(DINING_CATEGORY_ID)
                    .exclusions(List.of(exclusions))
                    .rule(anyRule())
                    .build();
        }

        @Test
        void 결제_카테고리가_제외되면_매칭되지_않는다() {
            BenefitCandidate candidate =
                    diningBenefitExcluding(new BenefitExclusion(ExclusionType.CATEGORY, "CAFE"));

            assertThat(matcher.matches(candidate, request(starbucksPayment(), null))).isFalse();
        }

        @Test
        void 대분류_제외는_하위_중분류_결제까지_제외한다() {
            // 대상 매칭이 계층으로 내려가듯 제외도 대칭으로 내려간다
            BenefitCandidate candidate = BenefitCandidate.builder()
                    .targetType(TargetType.ALL)
                    .exclusions(List.of(new BenefitExclusion(ExclusionType.CATEGORY, "DINING")))
                    .rule(anyRule())
                    .build();

            assertThat(matcher.matches(candidate, request(starbucksPayment(), null))).isFalse();
        }

        @Test
        void 결제_가맹점이_제외되면_매칭되지_않는다() {
            BenefitCandidate candidate =
                    diningBenefitExcluding(new BenefitExclusion(ExclusionType.MERCHANT, "STARBUCKS"));

            assertThat(matcher.matches(candidate, request(starbucksPayment(), null))).isFalse();
        }

        @Test
        void 결제수단이_제외되면_매칭되지_않는다() {
            BenefitCandidate candidate =
                    diningBenefitExcluding(new BenefitExclusion(ExclusionType.PAYMENT_TYPE, "SIMPLE_PAY"));

            assertThat(matcher.matches(candidate, request(starbucksPayment(), "SIMPLE_PAY"))).isFalse();
            assertThat(matcher.matches(candidate, request(starbucksPayment(), "CARD"))).isTrue();
        }

        @Test
        void 거래속성_제외는_추천_시점에_해석하지_않는다() {
            BenefitCandidate candidate =
                    diningBenefitExcluding(new BenefitExclusion(ExclusionType.TRANSACTION_ATTR, "INTEREST_FREE"));

            assertThat(matcher.matches(candidate, request(starbucksPayment(), null))).isTrue();
        }

        @Test
        void 제외_규칙이_여러_개면_하나만_걸려도_제외된다() {
            BenefitCandidate candidate = diningBenefitExcluding(
                    new BenefitExclusion(ExclusionType.CATEGORY, "DELIVERY"),
                    new BenefitExclusion(ExclusionType.MERCHANT, "STARBUCKS"));

            assertThat(matcher.matches(candidate, request(starbucksPayment(), null))).isFalse();
        }

        @Test
        void 걸리는_제외가_없으면_매칭된다() {
            BenefitCandidate candidate = diningBenefitExcluding(
                    new BenefitExclusion(ExclusionType.CATEGORY, "DELIVERY"),
                    new BenefitExclusion(ExclusionType.MERCHANT, "MCDONALDS"));

            assertThat(matcher.matches(candidate, request(starbucksPayment(), null))).isTrue();
        }
    }
}
