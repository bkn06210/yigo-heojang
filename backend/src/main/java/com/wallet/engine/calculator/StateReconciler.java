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

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 상태 대사 비교기 — 저장된 누적 상태를 원본(소비내역)에서 다시 구한 값과 맞춰 본다.
 *
 * 상태 테이블은 결제 때 더하고 취소 때 빼는 증분 갱신으로 유지된다. 매번 전체를 다시 합산하지
 * 않아 빠르지만, 한 번이라도 더하기와 빼기가 어긋나면 그 차이가 영영 남고 아무 오류도 나지 않는다.
 * 이 비교기가 그 어긋남을 찾는다.
 *
 * Spring·DB·시계에 의존하지 않는 순수 비교기다. 조회와 실적 재계산은 호출자가 끝내서 넘기고,
 * 여기서는 키를 맞춰 값을 비교하고 불일치를 분류하는 일만 한다.
 *
 * <b>행이 없는 것은 0으로 본다.</b> 엔진의 읽기 경로가 그렇게 읽기 때문이다(소진 행이 없으면 소진 0).
 * 그래서 취소로 0이 된 채 남은 행이나, 실적인정액이 0인 달에 행이 없는 것은 불일치가 아니다.
 * 어느 쪽이 없었는지는 버리지 않고 {@link DiscrepancyKind}로 남긴다 — 원인이 다르다.
 */
public final class StateReconciler {

    private static final Comparator<CardMonthKey> CARD_MONTH_ORDER = Comparator
            .comparingLong(CardMonthKey::userCardId)
            .thenComparing(CardMonthKey::baseYearMonth);

    private static final Comparator<BenefitUsageKey> BENEFIT_USAGE_ORDER = Comparator
            .comparingLong(BenefitUsageKey::userCardId)
            .thenComparing(BenefitUsageKey::baseYearMonth)
            .thenComparingLong(BenefitUsageKey::benefitId);

    /** 카드 월 상태(혜택 ID 없음)를 그 카드·그 달의 혜택 소진보다 앞에 둔다 */
    private static final Comparator<ReconciliationDiscrepancy> DISCREPANCY_ORDER = Comparator
            .comparingLong(ReconciliationDiscrepancy::userCardId)
            .thenComparing(ReconciliationDiscrepancy::baseYearMonth)
            .thenComparing(ReconciliationDiscrepancy::benefitId, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(ReconciliationDiscrepancy::target);

    public ReconciliationReport reconcile(ReconciliationInput input) {
        if (input == null) {
            throw new IllegalArgumentException("대사 입력은 필수다");
        }
        Tally tally = new Tally();
        int stateRowCount = reconcileCardMonths(input, tally);
        reconcileBenefitUsages(input, tally);

        // 같은 카드·같은 달의 불일치를 붙여 보여준다 — 실적과 소진이 함께 어긋났다면 원인이 하나일 가능성이 크다
        tally.discrepancies.sort(DISCREPANCY_ORDER);

        int expenseCount = input.sourceCardMonths().stream().mapToInt(SourceCardMonth::expenseCount).sum();
        return new ReconciliationReport(
                stateRowCount, input.storedBenefitUsages().size(), expenseCount,
                tally.comparisonCount, tally.matchedCount, tally.discrepancies, tally.unverifiables);
    }

    /**
     * 카드 월 상태(당월실적·통합한도·전월실적)를 맞춰 본다.
     *
     * @return 대조한 상태 행 수. 시작 연월보다 앞선 행은 이월의 기준으로만 쓰였으므로 세지 않는다
     */
    private int reconcileCardMonths(ReconciliationInput input, Tally tally) {
        Map<CardMonthKey, StoredCardMonth> storedByKey = input.storedCardMonths().stream()
                .collect(Collectors.toMap(
                        stored -> new CardMonthKey(stored.userCardId(), stored.baseYearMonth()),
                        Function.identity()));
        Map<CardMonthKey, SourceCardMonth> sourceByKey = input.sourceCardMonths().stream()
                .collect(Collectors.toMap(
                        source -> new CardMonthKey(source.userCardId(), source.baseYearMonth()),
                        Function.identity()));
        Map<CardMonthKey, Long> sharedLimitSourceByKey = sharedLimitSourceByKey(input.sourceBenefitUsages());

        TreeSet<CardMonthKey> keys = new TreeSet<>(CARD_MONTH_ORDER);
        storedByKey.keySet().stream()
                .filter(key -> key.baseYearMonth().compareTo(input.fromYearMonth()) >= 0)
                .forEach(keys::add);
        int stateRowCount = keys.size();
        keys.addAll(sourceByKey.keySet());
        keys.addAll(sharedLimitSourceByKey.keySet());

        for (CardMonthKey key : keys) {
            StoredCardMonth stored = storedByKey.get(key);
            SourceCardMonth source = sourceByKey.get(key);

            tally.compare(ReconciliationTarget.CURRENT_PERFORMANCE, key.userCardId(), null, key.baseYearMonth(),
                    stored == null ? null : stored.currentPerformanceAmount(),
                    source == null ? null : source.performanceAmount());
            tally.compare(ReconciliationTarget.SHARED_LIMIT_USED, key.userCardId(), null, key.baseYearMonth(),
                    stored == null ? null : stored.sharedLimitUsed(),
                    sharedLimitSourceByKey.get(key));

            // 이월은 행이 만들어질 때 한 번 정해지는 값이라, 행이 없으면 맞춰 볼 대상 자체가 없다.
            // 직전 월 행이 없으면 지난달 결제가 없었다는 뜻이므로 전월실적은 0이어야 한다
            if (stored != null) {
                StoredCardMonth previous = storedByKey.get(key.previousMonth());
                tally.compare(ReconciliationTarget.PREV_PERFORMANCE, key.userCardId(), null, key.baseYearMonth(),
                        stored.prevPerformanceAmount(),
                        previous == null ? null : previous.currentPerformanceAmount());
            }
        }
        return stateRowCount;
    }

    /**
     * 통합한도 사용액의 원본값 — 통합한도를 쓰는 혜택의 할인액만 (보유카드, 월)로 더한다.
     * 통합한도를 쓰지 않는 혜택까지 더하면 정상인 상태가 전부 불일치로 나온다.
     */
    private Map<CardMonthKey, Long> sharedLimitSourceByKey(List<SourceBenefitUsage> sourceBenefitUsages) {
        Map<CardMonthKey, Long> sumByKey = new HashMap<>();
        for (SourceBenefitUsage source : sourceBenefitUsages) {
            if (source.usesSharedLimit()) {
                sumByKey.merge(new CardMonthKey(source.userCardId(), source.baseYearMonth()),
                        source.discountAmount(), Long::sum);
            }
        }
        return sumByKey;
    }

    private void reconcileBenefitUsages(ReconciliationInput input, Tally tally) {
        Map<BenefitUsageKey, StoredBenefitUsage> storedByKey = input.storedBenefitUsages().stream()
                .collect(Collectors.toMap(
                        stored -> new BenefitUsageKey(stored.userCardId(), stored.benefitId(), stored.baseYearMonth()),
                        Function.identity()));
        Map<BenefitUsageKey, SourceBenefitUsage> sourceByKey = input.sourceBenefitUsages().stream()
                .collect(Collectors.toMap(
                        source -> new BenefitUsageKey(source.userCardId(), source.benefitId(), source.baseYearMonth()),
                        Function.identity()));

        TreeSet<BenefitUsageKey> keys = new TreeSet<>(BENEFIT_USAGE_ORDER);
        keys.addAll(storedByKey.keySet());
        keys.addAll(sourceByKey.keySet());

        for (BenefitUsageKey key : keys) {
            StoredBenefitUsage stored = storedByKey.get(key);
            SourceBenefitUsage source = sourceByKey.get(key);

            tally.compare(ReconciliationTarget.BENEFIT_USED_AMOUNT, key.userCardId(), key.benefitId(),
                    key.baseYearMonth(),
                    stored == null ? null : stored.usedAmount(),
                    source == null ? null : source.discountAmount());

            boolean stampBenefit = stored != null ? stored.stampBenefit() : source.stampBenefit();
            if (!stampBenefit) {
                tally.compare(ReconciliationTarget.BENEFIT_USED_COUNT, key.userCardId(), key.benefitId(),
                        key.baseYearMonth(),
                        stored == null ? null : stored.usedCount(),
                        source == null ? null : source.appliedCount());
            } else if (stored != null) {
                // 스탬프는 다른 혜택이 적용된 결제나 0원 결제에서도 찍힌다. 소비내역에는 적용된 혜택
                // 하나만 남으므로, 지급된 회차만 원본에 보이고 나머지 횟수는 어디에도 기록이 없다
                tally.unverifiables.add(new ReconciliationUnverifiable(
                        ReconciliationTarget.BENEFIT_USED_COUNT, key.userCardId(), key.benefitId(),
                        key.baseYearMonth(), stored.usedCount(),
                        source == null ? 0L : source.appliedCount()));
            }
        }
    }

    private record CardMonthKey(long userCardId, String baseYearMonth) {

        CardMonthKey previousMonth() {
            return new CardMonthKey(userCardId, YearMonth.parse(baseYearMonth).minusMonths(1).toString());
        }
    }

    private record BenefitUsageKey(long userCardId, long benefitId, String baseYearMonth) {
    }

    /** 대조 한 건씩의 결과를 모으는 누적기 */
    private static final class Tally {

        private int comparisonCount;
        private int matchedCount;
        private final List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();
        private final List<ReconciliationUnverifiable> unverifiables = new ArrayList<>();

        /**
         * 저장값과 원본값을 맞춰 본다. null은 "그쪽에 기록이 없다"이고 값으로는 0과 같다.
         * 양쪽 다 없으면 맞춰 볼 것이 없으므로 대조 건수에도 넣지 않는다.
         */
        void compare(ReconciliationTarget target, long userCardId, Long benefitId, String baseYearMonth,
                     Long storedValue, Long sourceValue) {
            if (storedValue == null && sourceValue == null) {
                return;
            }
            comparisonCount++;
            if (valueOrZero(storedValue) == valueOrZero(sourceValue)) {
                matchedCount++;
                return;
            }
            discrepancies.add(new ReconciliationDiscrepancy(
                    target, kindOf(storedValue, sourceValue), userCardId, benefitId, baseYearMonth,
                    storedValue, sourceValue));
        }

        private DiscrepancyKind kindOf(Long storedValue, Long sourceValue) {
            if (storedValue == null) {
                return DiscrepancyKind.SOURCE_WITHOUT_STATE;
            }
            if (sourceValue == null) {
                return DiscrepancyKind.STATE_WITHOUT_SOURCE;
            }
            return DiscrepancyKind.VALUE_MISMATCH;
        }

        private long valueOrZero(Long value) {
            return value == null ? 0L : value;
        }
    }
}
