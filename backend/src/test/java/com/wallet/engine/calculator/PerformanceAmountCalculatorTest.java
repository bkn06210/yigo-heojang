package com.wallet.engine.calculator;

import com.wallet.engine.model.PerformanceAmountResult;
import com.wallet.engine.model.PerformanceExclusion;
import com.wallet.engine.model.PerformanceExclusionType;
import com.wallet.engine.model.PerformanceTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PerformanceAmountCalculatorTest {

    private final PerformanceAmountCalculator calculator = new PerformanceAmountCalculator();

    @Nested
    @DisplayName("혜택이 지정한 실적 제외 (benefit.exclude_from_performance)")
    class BenefitExcluded {

        @Test
        void 그_혜택을_받은_거래는_제외_규칙이_없어도_빠진다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).discountAmount(500L).benefitExcludedFromPerformance(true).build(),
                    tx().amount(3000L).build());

            PerformanceAmountResult result = calculator.calculate(transactions, List.of());

            assertThat(result.amount()).isEqualTo(3000L);
        }

        /**
         * 카드 전체 규칙(TRANSACTION_ATTR='DISCOUNTED')으로 적으면 다른 혜택을 받은 거래까지 빠져
         * 실적이 실제보다 낮아진다. 혜택 단위 지정은 그 혜택을 받은 거래만 뺀다.
         */
        @Test
        void 다른_혜택을_받은_거래는_그대로_인정된다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).discountAmount(500L).benefitExcludedFromPerformance(true).build(),
                    tx().amount(7000L).discountAmount(300L).benefitExcludedFromPerformance(false).build());

            PerformanceAmountResult result = calculator.calculate(transactions, List.of());

            assertThat(result.amount()).isEqualTo(7000L);
        }
    }

    @Nested
    @DisplayName("기본 합산")
    class Basic {

        @Test
        void 제외_규칙이_없으면_전액이_실적으로_인정된다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).build(),
                    tx().amount(5000L).build());

            PerformanceAmountResult result = calculator.calculate(transactions, List.of());

            assertThat(result.amount()).isEqualTo(15000L);
            assertThat(result.ignored()).isEmpty();
        }

        @Test
        void 거래가_한_건도_없으면_0원이다() {
            PerformanceAmountResult result = calculator.calculate(List.of(), List.of());

            assertThat(result.amount()).isZero();
        }

        @Test
        void 취소된_거래는_합산에서_빠진다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).build(),
                    tx().amount(7000L).canceled(true).build());

            PerformanceAmountResult result = calculator.calculate(transactions, List.of());

            assertThat(result.amount()).isEqualTo(10000L);
        }

        @Test
        void 취소된_거래만_있으면_0원이다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).canceled(true).build());

            PerformanceAmountResult result = calculator.calculate(transactions, List.of());

            assertThat(result.amount()).isZero();
        }

        @Test
        void 제외_규칙이_null이면_빈_목록으로_보고_취소만_걸러낸다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).build(),
                    tx().amount(3000L).canceled(true).build());

            PerformanceAmountResult result = calculator.calculate(transactions, null);

            assertThat(result.amount()).isEqualTo(10000L);
        }
    }

    @Nested
    @DisplayName("카테고리 제외")
    class CategoryExclusion {

        @Test
        void 제외_카테고리와_같은_거래는_빠진다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).categoryCode("CAFE").build(),
                    tx().amount(5000L).categoryCode("MART").build());

            PerformanceAmountResult result = calculator.calculate(transactions, category("CAFE"));

            assertThat(result.amount()).isEqualTo(5000L);
        }

        @Test
        void 대분류_제외는_하위_중분류_거래까지_뺀다() {
            // 제외값이 대분류 LIVING이면 하위 중분류 UTILITY 결제도 빠져야 한다
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).categoryCode("UTILITY").parentCategoryCode("LIVING").build());

            PerformanceAmountResult result = calculator.calculate(transactions, category("LIVING"));

            assertThat(result.amount()).isZero();
        }

        @Test
        void 다른_대분류에_속한_거래는_빼지_않는다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).categoryCode("CAFE").parentCategoryCode("DINING").build());

            PerformanceAmountResult result = calculator.calculate(transactions, category("LIVING"));

            assertThat(result.amount()).isEqualTo(10000L);
        }

        @Test
        void 대분류_거래는_상위코드가_없어도_판정된다() {
            // 대분류 결제는 parentCategoryCode가 null — null이 제외 집합에 걸려선 안 된다
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).categoryCode("LIVING").parentCategoryCode(null).build());

            PerformanceAmountResult result = calculator.calculate(transactions, category("LIVING"));

            assertThat(result.amount()).isZero();
        }

        @Test
        void 존재하지_않는_카테고리_코드는_아무것도_빼지_않는다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).categoryCode("CAFE").build());

            PerformanceAmountResult result = calculator.calculate(transactions, category("NO_SUCH_CODE"));

            assertThat(result.amount()).isEqualTo(10000L);
        }
    }

    @Nested
    @DisplayName("결제수단 제외")
    class PaymentTypeExclusion {

        @Test
        void 제외_결제수단_거래는_빠진다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).paymentType("SIMPLE_PAY").build(),
                    tx().amount(6000L).paymentType("CARD").build());

            PerformanceAmountResult result = calculator.calculate(transactions, paymentType("SIMPLE_PAY"));

            assertThat(result.amount()).isEqualTo(6000L);
        }

        @Test
        void 결제수단이_없는_거래는_결제수단_규칙에_걸리지_않는다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).paymentType(null).build());

            PerformanceAmountResult result = calculator.calculate(transactions, paymentType("SIMPLE_PAY"));

            assertThat(result.amount()).isEqualTo(10000L);
        }
    }

    @Nested
    @DisplayName("거래속성 제외")
    class TransactionAttrExclusion {

        @Test
        void 무이자할부_거래는_빠진다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).interestFree(true).build(),
                    tx().amount(4000L).interestFree(false).build());

            PerformanceAmountResult result = calculator.calculate(transactions, transactionAttr("INTEREST_FREE"));

            assertThat(result.amount()).isEqualTo(4000L);
        }

        @Test
        void 무이자할부_규칙이_없으면_무이자할부_거래도_인정된다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).interestFree(true).build());

            PerformanceAmountResult result = calculator.calculate(transactions, List.of());

            assertThat(result.amount()).isEqualTo(10000L);
        }

        @Test
        void 할인받은_거래는_빠진다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).discountAmount(500L).build(),
                    tx().amount(3000L).discountAmount(0L).build());

            PerformanceAmountResult result = calculator.calculate(transactions, transactionAttr("DISCOUNTED"));

            assertThat(result.amount()).isEqualTo(3000L);
        }

        @Test
        void 할인액이_0원이면_할인받지_않은_것으로_보고_인정한다() {
            // 한도 소진으로 0원 적용됐거나 GIFT 0원인 거래는 "할인 안 받음" — discount_amount > 0이 기준
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).discountAmount(0L).build());

            PerformanceAmountResult result = calculator.calculate(transactions, transactionAttr("DISCOUNTED"));

            assertThat(result.amount()).isEqualTo(10000L);
        }

        @Test
        void 해외거래_규칙은_판정할_수_없어_무시된다() {
            // OVERSEAS는 expense에 대응 컬럼이 없다 — 예외가 아니라 무시 목록으로
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).build());

            PerformanceAmountResult result = calculator.calculate(transactions, transactionAttr("OVERSEAS"));

            assertThat(result.amount()).isEqualTo(10000L);
            assertThat(result.ignored()).hasSize(1);
            assertThat(result.ignored().get(0).rawValue()).isEqualTo("OVERSEAS");
        }

        @Test
        void 모르는_거래속성_값은_무시된다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).build());

            PerformanceAmountResult result = calculator.calculate(transactions, transactionAttr("WHATEVER"));

            assertThat(result.amount()).isEqualTo(10000L);
            assertThat(result.ignored()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("건당 최소금액")
    class MinTxnAmount {

        @Test
        void 기준_금액_미만_거래는_빠진다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(999L).build(),
                    tx().amount(5000L).build());

            PerformanceAmountResult result = calculator.calculate(transactions, minTxn("1000"));

            assertThat(result.amount()).isEqualTo(5000L);
        }

        @Test
        void 기준_금액과_같은_거래는_인정된다() {
            // 경계: 미만 제외이므로 == 기준은 인정
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(1000L).build());

            PerformanceAmountResult result = calculator.calculate(transactions, minTxn("1000"));

            assertThat(result.amount()).isEqualTo(1000L);
        }

        @Test
        void 기준이_여러_행이면_가장_큰_값이_적용된다() {
            // '1000'·'3000'이 함께 있으면 실효 기준은 3천 — 3천 미만 전부 제외
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(2000L).build(),
                    tx().amount(3000L).build());

            List<PerformanceExclusion> exclusions = List.of(
                    new PerformanceExclusion(PerformanceExclusionType.MIN_TXN_AMOUNT, "1000"),
                    new PerformanceExclusion(PerformanceExclusionType.MIN_TXN_AMOUNT, "3000"));

            PerformanceAmountResult result = calculator.calculate(transactions, exclusions);

            assertThat(result.amount()).isEqualTo(3000L);
        }

        @Test
        void 숫자가_아닌_기준값은_무시된다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(500L).build());

            PerformanceAmountResult result = calculator.calculate(transactions, minTxn("만원"));

            assertThat(result.amount()).isEqualTo(500L);
            assertThat(result.ignored()).hasSize(1);
        }

        @Test
        void 깨진_기준이_있어도_정상_기준은_그대로_적용된다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(500L).build(),
                    tx().amount(5000L).build());

            List<PerformanceExclusion> exclusions = List.of(
                    new PerformanceExclusion(PerformanceExclusionType.MIN_TXN_AMOUNT, "abc"),
                    new PerformanceExclusion(PerformanceExclusionType.MIN_TXN_AMOUNT, "1000"));

            PerformanceAmountResult result = calculator.calculate(transactions, exclusions);

            assertThat(result.amount()).isEqualTo(5000L);
            assertThat(result.ignored()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("규칙 조합")
    class Combined {

        @Test
        void 여러_규칙에_동시에_걸린_거래도_한_번만_빠진다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).categoryCode("CAFE").interestFree(true).build());

            List<PerformanceExclusion> exclusions = List.of(
                    new PerformanceExclusion(PerformanceExclusionType.CATEGORY, "CAFE"),
                    new PerformanceExclusion(PerformanceExclusionType.TRANSACTION_ATTR, "INTEREST_FREE"));

            PerformanceAmountResult result = calculator.calculate(transactions, exclusions);

            assertThat(result.amount()).isZero();
        }

        @Test
        void 규칙_하나에만_걸려도_빠진다() {
            List<PerformanceTransaction> transactions = List.of(
                    tx().amount(10000L).categoryCode("MART").interestFree(true).build());

            List<PerformanceExclusion> exclusions = List.of(
                    new PerformanceExclusion(PerformanceExclusionType.CATEGORY, "CAFE"),
                    new PerformanceExclusion(PerformanceExclusionType.TRANSACTION_ATTR, "INTEREST_FREE"));

            PerformanceAmountResult result = calculator.calculate(transactions, exclusions);

            assertThat(result.amount()).isZero();
        }
    }

    @Nested
    @DisplayName("입력 검증")
    class InputValidation {

        @Test
        void 거래_목록이_null이면_예외다() {
            assertThatIllegalArgumentException().isThrownBy(() -> calculator.calculate(null, List.of()));
        }
    }

    /** 1만원, 취소 안 됨, 할인 없음, 무이자 아님, 카테고리·결제수단 미지정 */
    private PerformanceTransaction.Builder tx() {
        return PerformanceTransaction.builder().expenseId(1L).amount(10000L);
    }

    private List<PerformanceExclusion> category(String code) {
        return List.of(new PerformanceExclusion(PerformanceExclusionType.CATEGORY, code));
    }

    private List<PerformanceExclusion> paymentType(String code) {
        return List.of(new PerformanceExclusion(PerformanceExclusionType.PAYMENT_TYPE, code));
    }

    private List<PerformanceExclusion> transactionAttr(String value) {
        return List.of(new PerformanceExclusion(PerformanceExclusionType.TRANSACTION_ATTR, value));
    }

    private List<PerformanceExclusion> minTxn(String value) {
        return List.of(new PerformanceExclusion(PerformanceExclusionType.MIN_TXN_AMOUNT, value));
    }
}
