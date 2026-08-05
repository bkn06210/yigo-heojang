package com.wallet.engine.assembler;

import com.wallet.engine.dao.dto.BenefitPeriodUsageRow;
import com.wallet.engine.dao.dto.BenefitUsageRow;
import com.wallet.engine.dao.dto.CardMonthlyStateRow;
import com.wallet.engine.model.BenefitUsage;
import com.wallet.engine.model.CardState;
import com.wallet.engine.model.PerformanceStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 상태 조회 row → 전월실적·CardState 변환기.
 *
 * CardState는 출처가 둘인 값을 합친 것이다:
 *   · 실적 판정 결과(PerformanceStatus) → performanceMet, sharedMonthlyLimit
 *   · DB 상태 테이블                    → sharedLimitUsed, usages
 * 계산기는 이 둘이 어디서 왔는지 몰라도 되도록 여기서 하나로 합쳐 넘긴다.
 *
 * 이 클래스가 다루는 '해석'은 두 가지다.
 *
 * <b>1. 전월실적 폴백</b> — {@link #resolvePrevPerformanceAmount}. 기준월 행이 없을 때
 * 직전월 행의 당월 누적액이 곧 전월실적이라는 규칙을 여기서만 안다.
 *
 * <b>2. 일 소진 리셋</b> — user_benefit_usage의 daily_* 컬럼은 last_applied_date 당일의 값이라,
 * 그 날짜가 오늘이 아니면 어제까지의 잔재다. 그대로 넘기면 오늘 첫 결제인데 일 한도가 이미 찬 것으로
 * 계산돼 혜택이 사라진다. BenefitUsage의 계약("오늘이 아니면 0")을 만족시키는 지점이 여기다.
 */
@Component
public class CardStateAssembler {

    /**
     * 카드 한 장의 전월실적인정액을 구한다. 저장된 집계값을 읽을 뿐 거래를 재합산하지 않는다.
     *
     * <pre>
     * 기준월 행 있음 → 그 행의 prev_performance_amount   (정산이 행을 만들 때 이월해 둔 값)
     * 기준월 행 없음 → 직전월 행의 current_performance_amount  (아직 이월 전이므로 원본을 직접 읽음)
     * 둘 다 없음     → 0
     * </pre>
     *
     * 셋째 갈래는 신규 카드만이 아니라 <b>달을 건너뛴 경우</b>도 포함한다 — 6월 거래가 있고 7월에
     * 거래가 없다가 8월에 조회하면 직전월(7월) 행이 없다. 이때 0은 오류가 아니라 정답이다.
     * 7월 실적이 실제로 0이기 때문이다.
     *
     * @param cardRows          이 카드의 상태 행들 (기준월·직전월이 섞여 있고, 없을 수도 있다)
     * @param baseYearMonth     기준 연월 (YYYY-MM)
     * @param previousYearMonth 직전 연월 (YYYY-MM)
     */
    public long resolvePrevPerformanceAmount(List<CardMonthlyStateRow> cardRows,
                                             String baseYearMonth, String previousYearMonth) {
        CardMonthlyStateRow baseMonthRow = findRow(cardRows, baseYearMonth);
        if (baseMonthRow != null) {
            return baseMonthRow.getPrevPerformanceAmount();
        }
        CardMonthlyStateRow previousMonthRow = findRow(cardRows, previousYearMonth);
        if (previousMonthRow != null) {
            return previousMonthRow.getCurrentPerformanceAmount();
        }
        return 0L;
    }

    /**
     * 카드 한 장의 상태를 조립한다.
     *
     * <b>직전월 행은 여기서 쓰지 않는다.</b> 전월실적 폴백에만 쓰이며, 소진액(shared_limit_used)을
     * 직전월 행에서 집으면 이번 달 한도가 이미 소진된 것으로 계산된다 — 에러 없이 금액만 틀리는 버그다.
     * 그래서 기준월 행만 골라 쓰고, 없으면 소진 0으로 본다(첫 결제 전이라 행이 없는 경우).
     * 행 생성은 추천(읽기 전용)이 아니라 정산의 몫이다.
     *
     * @param cardRows      이 카드의 상태 행들 (기준월·직전월이 섞여 있을 수 있다)
     * @param baseYearMonth 기준 연월 (YYYY-MM)
     * @param usageRows     이 카드의 기준월 혜택별 소진 행. 없는 혜택은 계산기가 BenefitUsage.empty로 본다
     * @param status        실적 판정 결과 (구간·통합한도)
     * @param today         일 소진 리셋 판정 기준일
     */
    public CardState toCardState(List<CardMonthlyStateRow> cardRows, String baseYearMonth,
                                 List<BenefitUsageRow> usageRows, PerformanceStatus status,
                                 LocalDate today) {
        return toCardState(cardRows, baseYearMonth, usageRows, List.of(), List.of(), Map.of(),
                status, null, today);
    }

    /**
     * @param quarterUsageRows   이 카드의 분기 합산 소진. 분기 한도 판정용
     * @param yearUsageRows      이 카드의 연 합산 소진. 연 한도 판정용
     * @param selectedOptionKeys 선택형 혜택 묶음별 그달의 선택 (option_group_code → option_key)
     * @param status             전월 실적으로 판정한 구간
     * @param quarterStatus      전분기 실적으로 판정한 구간. 분기 구간표가 없는 카드면 null
     */
    public CardState toCardState(List<CardMonthlyStateRow> cardRows, String baseYearMonth,
                                 List<BenefitUsageRow> usageRows,
                                 List<BenefitPeriodUsageRow> quarterUsageRows,
                                 List<BenefitPeriodUsageRow> yearUsageRows,
                                 Map<String, String> selectedOptionKeys,
                                 PerformanceStatus status, PerformanceStatus quarterStatus,
                                 LocalDate today) {
        if (status == null) {
            throw new IllegalArgumentException("실적 판정 결과(status)는 필수다");
        }
        if (today == null) {
            throw new IllegalArgumentException("기준일(today)은 필수다");
        }
        CardMonthlyStateRow baseMonthRow = findRow(cardRows, baseYearMonth);
        return new CardState(
                status,
                quarterStatus,
                baseMonthRow == null ? 0L : baseMonthRow.getSharedLimitUsed(),
                toUsages(usageRows, quarterUsageRows, yearUsageRows, today),
                selectedOptionKeys);
    }

    public List<BenefitUsage> toUsages(List<BenefitUsageRow> rows, LocalDate today) {
        return toUsages(rows, List.of(), List.of(), today);
    }

    /**
     * 세 출처(기준월·분기 합산·연 합산)를 혜택 id 기준으로 합친다.
     *
     * <b>혜택 id는 세 목록의 합집합이어야 한다.</b> 기준월 목록만 훑으면 "1월에 쓰고 3월에 조회"
     * 같은 경우에 연 소진이 통째로 빠진다 — 3월 행이 없어 목록에 안 잡히기 때문이다.
     * 그러면 연 한도가 비어 있는 것으로 계산돼 이미 다 쓴 혜택이 다시 지급된다.
     */
    public List<BenefitUsage> toUsages(List<BenefitUsageRow> monthRows,
                                       List<BenefitPeriodUsageRow> quarterRows,
                                       List<BenefitPeriodUsageRow> yearRows,
                                       LocalDate today) {
        Map<Long, BenefitUsageRow> monthByBenefit = indexByBenefitId(monthRows, BenefitUsageRow::getBenefitId);
        Map<Long, BenefitPeriodUsageRow> quarterByBenefit =
                indexByBenefitId(quarterRows, BenefitPeriodUsageRow::getBenefitId);
        Map<Long, BenefitPeriodUsageRow> yearByBenefit =
                indexByBenefitId(yearRows, BenefitPeriodUsageRow::getBenefitId);

        Set<Long> benefitIds = new LinkedHashSet<>();
        benefitIds.addAll(monthByBenefit.keySet());
        benefitIds.addAll(quarterByBenefit.keySet());
        benefitIds.addAll(yearByBenefit.keySet());

        return benefitIds.stream()
                .map(benefitId -> toUsage(benefitId, monthByBenefit.get(benefitId),
                        quarterByBenefit.get(benefitId), yearByBenefit.get(benefitId), today))
                .toList();
    }

    private <T> Map<Long, T> indexByBenefitId(List<T> rows, java.util.function.ToLongFunction<T> benefitIdOf) {
        if (rows == null) {
            return Map.of();
        }
        Map<Long, T> indexed = new LinkedHashMap<>();
        rows.forEach(row -> indexed.put(benefitIdOf.applyAsLong(row), row));
        return indexed;
    }

    /**
     * 월 소진은 원값 그대로, 일 소진은 last_applied_date가 오늘일 때만 유효한 값으로 접는다.
     * 한 번도 안 쓴 혜택은 last_applied_date가 NULL이라 자연히 0이 된다.
     */
    private BenefitUsage toUsage(long benefitId, BenefitUsageRow monthRow,
                                 BenefitPeriodUsageRow quarterRow, BenefitPeriodUsageRow yearRow,
                                 LocalDate today) {
        boolean appliedToday = monthRow != null && Objects.equals(monthRow.getLastAppliedDate(), today);
        return new BenefitUsage(
                benefitId,
                monthRow == null ? 0L : monthRow.getUsedAmount(),
                monthRow == null ? 0 : monthRow.getUsedCount(),
                appliedToday ? monthRow.getDailyUsedAmount() : 0L,
                appliedToday ? monthRow.getDailyUsedCount() : 0,
                quarterRow == null ? 0L : quarterRow.getUsedAmount(),
                quarterRow == null ? 0 : quarterRow.getUsedCount(),
                yearRow == null ? 0L : yearRow.getUsedAmount(),
                yearRow == null ? 0 : yearRow.getUsedCount());
    }

    /** 해당 연월의 상태 행을 찾는다. 없으면 null — 그 달에 아직 결제가 없었다는 뜻이다 */
    private CardMonthlyStateRow findRow(List<CardMonthlyStateRow> cardRows, String yearMonth) {
        if (cardRows == null) {
            return null;
        }
        return cardRows.stream()
                .filter(row -> yearMonth.equals(row.getBaseYearMonth()))
                .findFirst()
                .orElse(null);
    }
}
