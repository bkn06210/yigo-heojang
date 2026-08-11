package com.wallet.engine.assembler;

import com.wallet.engine.dao.dto.PerformanceExclusionRow;
import com.wallet.engine.dao.dto.PerformanceTierRow;
import com.wallet.engine.dao.dto.PerformanceTransactionRow;
import com.wallet.engine.model.PerformanceExclusion;
import com.wallet.engine.model.PerformanceExclusionType;
import com.wallet.engine.model.PerformancePeriod;
import com.wallet.engine.model.PerformanceTier;
import com.wallet.engine.model.PerformanceTransaction;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 조회 row → 불변 계산기 모델 변환기.
 *
 * DB 원값의 '해석'을 이 한 곳에 모은다 — 계산기 모델은 손대지 않는다.
 *   · payment_status → canceled(boolean)
 *   · is_interest_free 'Y'/'N' → interestFree(boolean)
 *   · exclusion_type 문자열 → PerformanceExclusionType (모르는 값은 fail-fast 예외)
 * DAO는 row만 반환하고 서비스는 도메인 모델만 다루도록, 그 경계 변환을 여기서 끝낸다.
 */
@Component
public class PerformanceInputAssembler {

    public List<PerformanceTransaction> toTransactions(List<PerformanceTransactionRow> rows) {
        return rows.stream().map(this::toTransaction).toList();
    }

    public PerformanceTransaction toTransaction(PerformanceTransactionRow row) {
        return PerformanceTransaction.builder()
                .expenseId(row.getExpenseId())
                .amount(row.getAmount())
                // 문자열 → boolean 접기는 엔진의 판정 규칙이 아니라 DB 경계 변환이므로 여기서 한다
                .canceled("CANCELED".equals(row.getPaymentStatus()))
                .categoryCode(row.getCategoryCode())
                .parentCategoryCode(row.getParentCategoryCode())
                .paymentType(row.getPaymentType())
                .interestFree("Y".equals(row.getIsInterestFree()))
                .discountAmount(row.getDiscountAmount())
                // 혜택 미적용 거래는 조인 결과가 NULL이라 자연히 false가 된다
                .benefitExcludedFromPerformance("Y".equals(row.getExcludeFromPerformance()))
                .build();
    }

    public List<PerformanceExclusion> toExclusions(List<PerformanceExclusionRow> rows) {
        return rows.stream().map(this::toExclusion).toList();
    }

    /**
     * 제외 규칙 변환. exclusion_type에는 CHECK 제약이 없어 오타난 타입이 DB에서 안 걸리므로,
     * PerformanceExclusionType.from이 모르는 값을 예외로 던져 스키마 위반을 드러낸다.
     */
    public PerformanceExclusion toExclusion(PerformanceExclusionRow row) {
        return new PerformanceExclusion(
                PerformanceExclusionType.from(row.getExclusionType()),
                row.getExclusionValue());
    }

    public List<PerformanceTier> toTiers(List<PerformanceTierRow> rows) {
        return rows.stream().map(this::toTier).toList();
    }

    /**
     * 한 기간 축의 구간표만 골라낸다.
     *
     * 판정은 같은 기간의 행끼리만 비교해야 한다 — 전월 축과 전분기 축을 섞으면 전분기 100만원
     * 조건이 전월 금액으로 판정돼, 충족한 회원이 미충족으로 읽힌다.
     * 그 기간의 구간표가 아예 없으면 빈 목록이며, 호출자가 "판정 불가"로 다룬다.
     */
    public List<PerformanceTier> toTiers(List<PerformanceTierRow> rows, PerformancePeriod period) {
        return rows.stream()
                .map(this::toTier)
                .filter(tier -> tier.periodType() == period)
                .toList();
    }

    public PerformanceTier toTier(PerformanceTierRow row) {
        // sharedMonthlyLimit은 Long 그대로 넘긴다 — NULL(통합한도 없음)≠0(혜택 없음)을 유지한다
        return new PerformanceTier(
                row.getTierId(),
                PerformancePeriod.from(row.getPeriodType()),
                row.getMinPerformanceAmount(),
                row.getSharedMonthlyLimit());
    }
}
