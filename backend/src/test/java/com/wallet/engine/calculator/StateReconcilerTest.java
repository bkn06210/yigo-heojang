package com.wallet.engine.calculator;

import com.wallet.engine.model.DiscrepancyKind;
import com.wallet.engine.model.ReconciliationDiscrepancy;
import com.wallet.engine.model.ReconciliationInput;
import com.wallet.engine.model.ReconciliationInput.SourceBenefitUsage;
import com.wallet.engine.model.ReconciliationInput.SourceCardMonth;
import com.wallet.engine.model.ReconciliationInput.StoredBenefitUsage;
import com.wallet.engine.model.ReconciliationInput.StoredCardMonth;
import com.wallet.engine.model.ReconciliationReport;
import com.wallet.engine.model.ReconciliationTarget;
import com.wallet.engine.model.ReconciliationUnverifiable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class StateReconcilerTest {

    private static final long CARD = 1L;
    private static final long CAFE_BENEFIT = 10L;
    private static final long MART_BENEFIT = 11L;
    private static final long STAMP_BENEFIT = 20L;
    private static final String JULY = "2026-07";
    private static final String AUGUST = "2026-08";

    private final StateReconciler reconciler = new StateReconciler();

    @Nested
    @DisplayName("혜택 소진 (user_benefit_usage)")
    class BenefitUsage {

        @Test
        void 소진액과_횟수가_원본_합계와_같으면_일치다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedUsage(CAFE_BENEFIT, AUGUST, 3_000, 2)
                    .sourceUsage(CAFE_BENEFIT, AUGUST, 3_000, 2)
                    .build());

            assertThat(report.consistent()).isTrue();
            // 금액 한 건 + 횟수 한 건
            assertThat(report.comparisonCount()).isEqualTo(2);
            assertThat(report.matchedCount()).isEqualTo(2);
            assertThat(report.usageRowCount()).isEqualTo(1);
        }

        @Test
        void 소진액이_원본_할인액_합과_다르면_값_불일치다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedUsage(CAFE_BENEFIT, AUGUST, 3_500, 2)
                    .sourceUsage(CAFE_BENEFIT, AUGUST, 3_000, 2)
                    .build());

            assertThat(report.discrepancies()).singleElement().satisfies(discrepancy -> {
                assertThat(discrepancy.target()).isEqualTo(ReconciliationTarget.BENEFIT_USED_AMOUNT);
                assertThat(discrepancy.kind()).isEqualTo(DiscrepancyKind.VALUE_MISMATCH);
                assertThat(discrepancy.userCardId()).isEqualTo(CARD);
                assertThat(discrepancy.benefitId()).isEqualTo(CAFE_BENEFIT);
                assertThat(discrepancy.baseYearMonth()).isEqualTo(AUGUST);
                assertThat(discrepancy.storedValue()).isEqualTo(3_500L);
                assertThat(discrepancy.sourceValue()).isEqualTo(3_000L);
                // 양수 = 상태가 원본보다 많이 쌓였다 → 한도가 실제보다 덜 남아 보인다
                assertThat(discrepancy.difference()).isEqualTo(500L);
            });
            assertThat(report.matchedCount()).isEqualTo(1);
        }

        @Test
        void 횟수만_다르면_횟수_불일치_한_건만_나온다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedUsage(CAFE_BENEFIT, AUGUST, 3_000, 3)
                    .sourceUsage(CAFE_BENEFIT, AUGUST, 3_000, 2)
                    .build());

            assertThat(report.discrepancies()).singleElement().satisfies(discrepancy -> {
                assertThat(discrepancy.target()).isEqualTo(ReconciliationTarget.BENEFIT_USED_COUNT);
                assertThat(discrepancy.difference()).isEqualTo(1L);
            });
        }

        @Test
        void 소진_행에_값이_있는데_적용된_소비내역이_없으면_원본_없는_상태다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedUsage(CAFE_BENEFIT, AUGUST, 1_500, 1)
                    .build());

            assertThat(report.discrepancies()).hasSize(2).allSatisfy(discrepancy -> {
                assertThat(discrepancy.kind()).isEqualTo(DiscrepancyKind.STATE_WITHOUT_SOURCE);
                assertThat(discrepancy.sourceValue()).isNull();
            });
            assertThat(report.discrepancies()).extracting(ReconciliationDiscrepancy::target)
                    .containsExactly(ReconciliationTarget.BENEFIT_USED_AMOUNT,
                            ReconciliationTarget.BENEFIT_USED_COUNT);
        }

        /** 취소는 행을 지우지 않고 값을 0으로 되돌린다. 남은 0은 "쓴 적 없음"과 같은 뜻이라 불일치가 아니다 */
        @Test
        void 값이_0인_소진_행은_원본이_없어도_일치다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedUsage(CAFE_BENEFIT, AUGUST, 0, 0)
                    .build());

            assertThat(report.consistent()).isTrue();
            assertThat(report.comparisonCount()).isEqualTo(2);
        }

        @Test
        void 원본에는_혜택이_적용됐는데_소진_행이_없으면_상태_없는_원본이다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .sourceUsage(CAFE_BENEFIT, AUGUST, 1_000, 1)
                    .build());

            assertThat(report.discrepancies()).hasSize(2).allSatisfy(discrepancy -> {
                assertThat(discrepancy.kind()).isEqualTo(DiscrepancyKind.SOURCE_WITHOUT_STATE);
                assertThat(discrepancy.storedValue()).isNull();
            });
            // 음수 = 상태가 원본보다 덜 쌓였다 → 한도를 넘겨 혜택을 줄 수 있다
            assertThat(report.discrepancies().get(0).difference()).isEqualTo(-1_000L);
        }

        /** 결제 후 취소된 건만 있으면 원본 합계가 0이다. 상태도 0으로 되돌아왔어야 한다 */
        @Test
        void 전부_취소된_혜택인데_소진이_남아_있으면_값_불일치다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedUsage(CAFE_BENEFIT, AUGUST, 1_000, 1)
                    .sourceUsage(CAFE_BENEFIT, AUGUST, 0, 0)
                    .build());

            assertThat(report.discrepancies()).hasSize(2)
                    .allSatisfy(discrepancy ->
                            assertThat(discrepancy.kind()).isEqualTo(DiscrepancyKind.VALUE_MISMATCH));
        }

        @Test
        void 같은_혜택이라도_달이_다르면_따로_맞춰_본다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedUsage(CAFE_BENEFIT, JULY, 1_000, 1)
                    .storedUsage(CAFE_BENEFIT, AUGUST, 2_000, 2)
                    .sourceUsage(CAFE_BENEFIT, JULY, 2_000, 2)
                    .sourceUsage(CAFE_BENEFIT, AUGUST, 1_000, 1)
                    .build());

            // 두 달을 합치면 같지만(3,000원·3회) 달마다는 전부 어긋난다
            assertThat(report.discrepancies()).hasSize(4);
        }
    }

    @Nested
    @DisplayName("스탬프 횟수 — 원본으로 복원할 수 없는 값")
    class StampCount {

        /**
         * 스탬프는 혜택이 지급되지 않은 결제에서도 찍히는데 소비내역에는 흔적이 없다.
         * 저장 4회 중 원본에 보이는 것은 지급된 1회뿐이므로, 다르다고 해서 틀렸다고 할 수 없다.
         */
        @Test
        void 스탬프_횟수는_원본과_달라도_불일치가_아니라_대조_불가다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedStampUsage(STAMP_BENEFIT, AUGUST, 2_000, 5)
                    .sourceStampUsage(STAMP_BENEFIT, AUGUST, 2_000, 1)
                    .build());

            assertThat(report.consistent()).isTrue();
            assertThat(report.unverifiables()).singleElement().satisfies(unverifiable -> {
                assertThat(unverifiable.target()).isEqualTo(ReconciliationTarget.BENEFIT_USED_COUNT);
                assertThat(unverifiable.benefitId()).isEqualTo(STAMP_BENEFIT);
                assertThat(unverifiable.storedValue()).isEqualTo(5L);
                assertThat(unverifiable.tracedValue()).isEqualTo(1L);
            });
            // 금액만 대조 건수에 들어간다 — 대조 불가는 맞춰 본 것이 아니다
            assertThat(report.comparisonCount()).isEqualTo(1);
        }

        @Test
        void 지급된_적_없이_찍히기만_한_스탬프도_대조_불가로_남는다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedStampUsage(STAMP_BENEFIT, AUGUST, 0, 3)
                    .build());

            assertThat(report.consistent()).isTrue();
            assertThat(report.unverifiables()).extracting(ReconciliationUnverifiable::tracedValue)
                    .containsExactly(0L);
        }

        /** 횟수는 복원하지 못해도 금액은 소비내역에 남는다. 스탬프라고 금액 대조까지 건너뛰면 안 된다 */
        @Test
        void 스탬프라도_지급액은_원본과_맞춰_본다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedStampUsage(STAMP_BENEFIT, AUGUST, 4_000, 5)
                    .sourceStampUsage(STAMP_BENEFIT, AUGUST, 2_000, 1)
                    .build());

            assertThat(report.discrepancies()).singleElement().satisfies(discrepancy -> {
                assertThat(discrepancy.target()).isEqualTo(ReconciliationTarget.BENEFIT_USED_AMOUNT);
                assertThat(discrepancy.difference()).isEqualTo(2_000L);
            });
        }
    }

    @Nested
    @DisplayName("카드 월 상태 (user_card_monthly_state)")
    class CardMonth {

        @Test
        void 당월_실적이_재계산값과_같으면_일치다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedState(AUGUST, 0, 50_000, 0)
                    .sourceMonth(AUGUST, 3, 50_000)
                    .build());

            assertThat(report.consistent()).isTrue();
            assertThat(report.stateRowCount()).isEqualTo(1);
            assertThat(report.expenseCount()).isEqualTo(3);
        }

        @Test
        void 당월_실적이_재계산값과_다르면_값_불일치다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedState(AUGUST, 0, 60_000, 0)
                    .sourceMonth(AUGUST, 3, 50_000)
                    .build());

            assertThat(report.discrepancies()).singleElement().satisfies(discrepancy -> {
                assertThat(discrepancy.target()).isEqualTo(ReconciliationTarget.CURRENT_PERFORMANCE);
                assertThat(discrepancy.kind()).isEqualTo(DiscrepancyKind.VALUE_MISMATCH);
                assertThat(discrepancy.benefitId()).isNull();
                assertThat(discrepancy.difference()).isEqualTo(10_000L);
            });
        }

        @Test
        void 실적이_쌓여_있는데_그_달_소비내역이_없으면_원본_없는_상태다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedState(AUGUST, 0, 120_000, 0)
                    .build());

            assertThat(report.discrepancies()).singleElement().satisfies(discrepancy -> {
                assertThat(discrepancy.target()).isEqualTo(ReconciliationTarget.CURRENT_PERFORMANCE);
                assertThat(discrepancy.kind()).isEqualTo(DiscrepancyKind.STATE_WITHOUT_SOURCE);
            });
        }

        @Test
        void 소비내역은_있는데_월_상태_행이_없으면_상태_없는_원본이다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .sourceMonth(AUGUST, 2, 30_000)
                    .build());

            assertThat(report.discrepancies()).singleElement().satisfies(discrepancy -> {
                assertThat(discrepancy.target()).isEqualTo(ReconciliationTarget.CURRENT_PERFORMANCE);
                assertThat(discrepancy.kind()).isEqualTo(DiscrepancyKind.SOURCE_WITHOUT_STATE);
                assertThat(discrepancy.storedValue()).isNull();
                assertThat(discrepancy.sourceValue()).isEqualTo(30_000L);
            });
            assertThat(report.stateRowCount()).isZero();
        }

        /** 그 달 거래가 전부 실적 제외면 인정액이 0이다. 행이 없는 것과 값이 같으므로 어긋난 것이 없다 */
        @Test
        void 실적인정액이_0인_달은_상태_행이_없어도_일치다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .sourceMonth(AUGUST, 2, 0)
                    .build());

            assertThat(report.consistent()).isTrue();
        }

        @Test
        void 통합한도_사용액은_통합한도를_쓰는_혜택의_할인액만_더한다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedState(AUGUST, 0, 50_000, 3_000)
                    .sourceMonth(AUGUST, 3, 50_000)
                    .storedUsage(CAFE_BENEFIT, AUGUST, 2_000, 1)
                    .storedUsage(MART_BENEFIT, AUGUST, 1_000, 1)
                    .storedUsage(STAMP_BENEFIT, AUGUST, 700, 1)
                    .sourceUsage(CAFE_BENEFIT, AUGUST, 2_000, 1, true)
                    .sourceUsage(MART_BENEFIT, AUGUST, 1_000, 1, true)
                    // 통합한도를 쓰지 않는 혜택 — 합계에 들어가면 3,700원이 되어 불일치로 나온다
                    .sourceUsage(STAMP_BENEFIT, AUGUST, 700, 1, false)
                    .build());

            assertThat(report.consistent()).isTrue();
        }

        @Test
        void 통합한도_사용액이_원본과_다르면_값_불일치다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedState(AUGUST, 0, 50_000, 5_000)
                    .sourceMonth(AUGUST, 3, 50_000)
                    .storedUsage(CAFE_BENEFIT, AUGUST, 2_000, 1)
                    .sourceUsage(CAFE_BENEFIT, AUGUST, 2_000, 1, true)
                    .build());

            assertThat(report.discrepancies()).singleElement().satisfies(discrepancy -> {
                assertThat(discrepancy.target()).isEqualTo(ReconciliationTarget.SHARED_LIMIT_USED);
                assertThat(discrepancy.kind()).isEqualTo(DiscrepancyKind.VALUE_MISMATCH);
                assertThat(discrepancy.storedValue()).isEqualTo(5_000L);
                assertThat(discrepancy.sourceValue()).isEqualTo(2_000L);
            });
        }

        @Test
        void 통합한도가_쌓여_있는데_통합한도_혜택이_적용된_소비내역이_없으면_원본_없는_상태다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedState(AUGUST, 0, 50_000, 2_000)
                    .sourceMonth(AUGUST, 3, 50_000)
                    .build());

            assertThat(report.discrepancies()).singleElement().satisfies(discrepancy -> {
                assertThat(discrepancy.target()).isEqualTo(ReconciliationTarget.SHARED_LIMIT_USED);
                assertThat(discrepancy.kind()).isEqualTo(DiscrepancyKind.STATE_WITHOUT_SOURCE);
            });
        }
    }

    @Nested
    @DisplayName("전월실적 이월 (prev_performance_amount)")
    class PrevPerformance {

        @Test
        void 전월실적이_직전_월_행의_당월_누적과_같으면_일치다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedState(JULY, 0, 350_000, 0)
                    .sourceMonth(JULY, 5, 350_000)
                    .storedState(AUGUST, 350_000, 10_000, 0)
                    .sourceMonth(AUGUST, 1, 10_000)
                    .from(JULY)
                    .build());

            assertThat(report.consistent()).isTrue();
        }

        @Test
        void 전월실적이_직전_월_누적과_다르면_값_불일치다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedState(JULY, 0, 350_000, 0)
                    .sourceMonth(JULY, 5, 350_000)
                    .storedState(AUGUST, 300_000, 10_000, 0)
                    .sourceMonth(AUGUST, 1, 10_000)
                    .from(JULY)
                    .build());

            assertThat(report.discrepancies()).singleElement().satisfies(discrepancy -> {
                assertThat(discrepancy.target()).isEqualTo(ReconciliationTarget.PREV_PERFORMANCE);
                assertThat(discrepancy.baseYearMonth()).isEqualTo(AUGUST);
                assertThat(discrepancy.storedValue()).isEqualTo(300_000L);
                assertThat(discrepancy.sourceValue()).isEqualTo(350_000L);
                assertThat(discrepancy.difference()).isEqualTo(-50_000L);
            });
        }

        /** 직전 월 행이 없다는 것은 지난달 결제가 없었다는 뜻이다. 이월할 것이 없으니 0이어야 한다 */
        @Test
        void 직전_월_행이_없는데_전월실적이_있으면_원본_없는_상태다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedState(AUGUST, 350_000, 10_000, 0)
                    .sourceMonth(AUGUST, 1, 10_000)
                    .build());

            assertThat(report.discrepancies()).singleElement().satisfies(discrepancy -> {
                assertThat(discrepancy.target()).isEqualTo(ReconciliationTarget.PREV_PERFORMANCE);
                assertThat(discrepancy.kind()).isEqualTo(DiscrepancyKind.STATE_WITHOUT_SOURCE);
            });
        }

        @Test
        void 직전_월_행이_없고_전월실적도_0이면_일치다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedState(AUGUST, 0, 10_000, 0)
                    .sourceMonth(AUGUST, 1, 10_000)
                    .build());

            assertThat(report.consistent()).isTrue();
        }

        /**
         * 대조 구간이 8월부터면 7월 행은 8월의 전월실적을 맞춰 볼 기준일 뿐이다.
         * 7월의 소비내역은 읽지 않았으므로, 7월 행까지 대조하면 멀쩡한 실적이 "원본 없는 상태"로 나온다.
         */
        @Test
        void 구간_앞의_행은_이월의_기준으로만_쓰고_대조하지_않는다() {
            ReconciliationReport report = reconciler.reconcile(input()
                    .storedState(JULY, 0, 350_000, 4_000)
                    .storedState(AUGUST, 350_000, 10_000, 0)
                    .sourceMonth(AUGUST, 1, 10_000)
                    .from(AUGUST)
                    .build());

            assertThat(report.consistent()).isTrue();
            assertThat(report.stateRowCount()).isEqualTo(1);
            // 8월 행의 당월실적 · 통합한도 · 전월실적
            assertThat(report.comparisonCount()).isEqualTo(3);
        }
    }

    @Test
    void 다른_카드의_값을_섞어_맞추지_않는다() {
        long otherCard = 2L;
        ReconciliationReport report = reconciler.reconcile(new ReconciliationInput(
                AUGUST,
                List.of(new StoredCardMonth(CARD, AUGUST, 0, 10_000, 0),
                        new StoredCardMonth(otherCard, AUGUST, 0, 20_000, 0)),
                List.of(),
                List.of(new SourceCardMonth(CARD, AUGUST, 1, 20_000),
                        new SourceCardMonth(otherCard, AUGUST, 1, 10_000)),
                List.of()));

        // 두 카드를 합치면 같지만(30,000원) 카드마다는 둘 다 어긋난다
        assertThat(report.discrepancies()).extracting(ReconciliationDiscrepancy::userCardId)
                .containsExactly(CARD, otherCard);
    }

    @Test
    void 불일치는_카드_월_순으로_정렬되고_카드_상태가_혜택_소진보다_앞에_온다() {
        ReconciliationReport report = reconciler.reconcile(input()
                .storedUsage(CAFE_BENEFIT, AUGUST, 9_000, 1)
                .sourceUsage(CAFE_BENEFIT, AUGUST, 1_000, 1)
                .storedState(AUGUST, 0, 99_000, 0)
                .sourceMonth(AUGUST, 1, 10_000)
                .storedState(JULY, 0, 77_000, 0)
                .sourceMonth(JULY, 1, 10_000)
                .from(JULY)
                .build());

        assertThat(report.discrepancies())
                .extracting(discrepancy -> discrepancy.baseYearMonth() + " " + discrepancy.target())
                .containsExactly(
                        JULY + " CURRENT_PERFORMANCE",
                        AUGUST + " CURRENT_PERFORMANCE",
                        // 7월 누적이 77,000인데 8월 전월실적이 0 — 이월도 함께 어긋난다
                        AUGUST + " PREV_PERFORMANCE",
                        AUGUST + " BENEFIT_USED_AMOUNT");
    }

    @Test
    void 대조할_것이_없으면_빈_결과다() {
        ReconciliationReport report = reconciler.reconcile(input().build());

        assertThat(report.consistent()).isTrue();
        assertThat(report.comparisonCount()).isZero();
        assertThat(report.unverifiableCount()).isZero();
    }

    @Test
    void 입력이_없으면_예외다() {
        assertThatIllegalArgumentException().isThrownBy(() -> reconciler.reconcile(null));
    }

    // ── 입력 조립 ──────────────────────────────────────────────────────────

    private static InputBuilder input() {
        return new InputBuilder();
    }

    /** 카드 한 장(CARD)의 입력을 줄 단위로 쌓는다. 시작 연월을 정하지 않으면 8월부터 대조한다 */
    private static final class InputBuilder {

        private String fromYearMonth = AUGUST;
        private final List<StoredCardMonth> storedCardMonths = new ArrayList<>();
        private final List<StoredBenefitUsage> storedBenefitUsages = new ArrayList<>();
        private final List<SourceCardMonth> sourceCardMonths = new ArrayList<>();
        private final List<SourceBenefitUsage> sourceBenefitUsages = new ArrayList<>();

        InputBuilder from(String yearMonth) {
            this.fromYearMonth = yearMonth;
            return this;
        }

        InputBuilder storedState(String yearMonth, long prevPerformance, long currentPerformance,
                                 long sharedLimitUsed) {
            storedCardMonths.add(new StoredCardMonth(
                    CARD, yearMonth, prevPerformance, currentPerformance, sharedLimitUsed));
            return this;
        }

        InputBuilder sourceMonth(String yearMonth, int expenseCount, long performanceAmount) {
            sourceCardMonths.add(new SourceCardMonth(CARD, yearMonth, expenseCount, performanceAmount));
            return this;
        }

        InputBuilder storedUsage(long benefitId, String yearMonth, long usedAmount, long usedCount) {
            storedBenefitUsages.add(new StoredBenefitUsage(
                    CARD, benefitId, yearMonth, usedAmount, usedCount, false));
            return this;
        }

        InputBuilder storedStampUsage(long benefitId, String yearMonth, long usedAmount, long usedCount) {
            storedBenefitUsages.add(new StoredBenefitUsage(
                    CARD, benefitId, yearMonth, usedAmount, usedCount, true));
            return this;
        }

        InputBuilder sourceUsage(long benefitId, String yearMonth, long discountAmount, long appliedCount) {
            return sourceUsage(benefitId, yearMonth, discountAmount, appliedCount, false);
        }

        InputBuilder sourceUsage(long benefitId, String yearMonth, long discountAmount, long appliedCount,
                                 boolean usesSharedLimit) {
            sourceBenefitUsages.add(new SourceBenefitUsage(
                    CARD, benefitId, yearMonth, discountAmount, appliedCount, usesSharedLimit, false));
            return this;
        }

        InputBuilder sourceStampUsage(long benefitId, String yearMonth, long discountAmount, long appliedCount) {
            sourceBenefitUsages.add(new SourceBenefitUsage(
                    CARD, benefitId, yearMonth, discountAmount, appliedCount, false, true));
            return this;
        }

        ReconciliationInput build() {
            return new ReconciliationInput(fromYearMonth, storedCardMonths, storedBenefitUsages,
                    sourceCardMonths, sourceBenefitUsages);
        }
    }
}
