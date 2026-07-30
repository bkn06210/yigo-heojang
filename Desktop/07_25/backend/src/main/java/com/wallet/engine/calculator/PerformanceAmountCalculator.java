package com.wallet.engine.calculator;

import com.wallet.engine.model.IgnoredExclusion;
import com.wallet.engine.model.PerformanceAmountResult;
import com.wallet.engine.model.PerformanceExclusion;
import com.wallet.engine.model.PerformanceTransaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 전월실적 계산기 — 거래 목록에서 제외 규칙에 걸리지 않는 건의 승인액을 합산한다.
 *
 * 카드사가 전월실적 값을 주지 않으므로 엔진이 직접 계산한다. 당월 누적 실적
 * (current_performance_amount)도 같은 함수로 계산한다 — 대상 기간만 다르고(DAO의 WHERE가 결정)
 * 판정 규칙은 같다. 그래서 이 클래스는 시계를 모른다.
 *
 * Spring·DB·시계에 의존하지 않는 순수 계산기다. 제외 규칙 값에 FK가 없어(코드 문자열)
 * 오타·판정 불가값이 섞일 수 있는데, 알려진 유형인데 값을 해석할 수 없으면(OVERSEAS,
 * 숫자 파싱 실패) 계산을 죽이지 않고 IgnoredExclusion 목록에 담아 반환한다. 로깅은 하지 않는다 —
 * 그 목록을 받은 서비스 계층이 운영자 로그를, 챗봇이 사용자 답변을 만든다.
 * (모르는 유형은 여기 오기 전에 PerformanceExclusionType.from에서 예외로 걸린다)
 */
public final class PerformanceAmountCalculator {

    public PerformanceAmountResult calculate(List<PerformanceTransaction> transactions,
                                             List<PerformanceExclusion> exclusions) {
        if (transactions == null) {
            throw new IllegalArgumentException("거래 목록은 필수다 (없으면 빈 목록)");
        }
        CompiledExclusions compiled = compile(exclusions);

        long sum = 0L;
        for (PerformanceTransaction transaction : transactions) {
            if (transaction.isCanceled()) {
                continue;
            }
            if (compiled.excludes(transaction)) {
                continue;
            }
            sum += transaction.getAmount();
        }
        return new PerformanceAmountResult(sum, compiled.ignored());
    }

    /**
     * 제외 규칙을 거래 루프 밖에서 한 번만 정규화한다. 이게 없으면 깨진 행 하나가
     * 거래 N건마다 파싱 실패를 반복해 무시 목록이 N배로 부푼다.
     */
    private CompiledExclusions compile(List<PerformanceExclusion> exclusions) {
        CompiledExclusions compiled = new CompiledExclusions();
        if (exclusions == null) {
            return compiled;
        }
        for (PerformanceExclusion exclusion : exclusions) {
            compiled.add(exclusion);
        }
        return compiled;
    }

    /** 정규화된 제외 규칙. 규칙들은 OR이고 결과는 "제외/인정" 이진값이라 판정 순서는 합계에 무관하다. */
    private static final class CompiledExclusions {

        private final Set<String> excludedCategoryCodes = new HashSet<>();
        private final Set<String> excludedPaymentTypes = new HashSet<>();
        private boolean excludeInterestFree;
        private boolean excludeDiscounted;
        private Long minTxnAmount;
        private final List<IgnoredExclusion> ignored = new ArrayList<>();

        void add(PerformanceExclusion exclusion) {
            switch (exclusion.type()) {
                case CATEGORY -> excludedCategoryCodes.add(exclusion.value());
                case PAYMENT_TYPE -> excludedPaymentTypes.add(exclusion.value());
                case TRANSACTION_ATTR -> addTransactionAttr(exclusion);
                case MIN_TXN_AMOUNT -> addMinTxnAmount(exclusion);
            }
        }

        private void addTransactionAttr(PerformanceExclusion exclusion) {
            switch (exclusion.value()) {
                case "INTEREST_FREE" -> excludeInterestFree = true;
                case "DISCOUNTED" -> excludeDiscounted = true;
                // OVERSEAS는 expense에 대응 컬럼이 없어 판정 불가. 오타난 값도 여기로 온다
                default -> ignore(exclusion, "판정할 수 없는 거래 속성");
            }
        }

        private void addMinTxnAmount(PerformanceExclusion exclusion) {
            long parsed;
            try {
                parsed = Long.parseLong(exclusion.value().trim());
            } catch (NumberFormatException e) {
                ignore(exclusion, "숫자가 아닌 최소금액 기준");
                return;
            }
            if (parsed < 0) {
                ignore(exclusion, "음수 최소금액 기준");
                return;
            }
            // 여러 행이면 최댓값 채택 — '10000'·'30000'이 함께 있으면 실효 기준은 3만이다(OR의 결과)
            minTxnAmount = (minTxnAmount == null) ? parsed : Math.max(minTxnAmount, parsed);
        }

        private void ignore(PerformanceExclusion exclusion, String reason) {
            ignored.add(new IgnoredExclusion(exclusion.type().name(), exclusion.value(), reason));
        }

        boolean excludes(PerformanceTransaction transaction) {
            if (minTxnAmount != null && transaction.getAmount() < minTxnAmount) {
                return true;
            }
            if (excludeInterestFree && transaction.isInterestFree()) {
                return true;
            }
            if (excludeDiscounted && transaction.getDiscountAmount() > 0) {
                return true;
            }
            if (excludedPaymentTypes.contains(transaction.getPaymentType())) {
                return true;
            }
            // 대분류 제외는 하위 중분류 결제까지 걸러야 하므로 상위 코드도 함께 본다(상향 매칭)
            return excludedCategoryCodes.contains(transaction.getCategoryCode())
                    || excludedCategoryCodes.contains(transaction.getParentCategoryCode());
        }

        List<IgnoredExclusion> ignored() {
            return ignored;
        }
    }
}
