package com.wallet.engine.service;

import com.wallet.engine.assembler.PerformanceInputAssembler;
import com.wallet.engine.calculator.PerformanceAmountCalculator;
import com.wallet.engine.calculator.StateReconciler;
import com.wallet.engine.dao.PerformanceMapper;
import com.wallet.engine.dao.ReconciliationMapper;
import com.wallet.engine.dao.dto.CardMonthlyStateRow;
import com.wallet.engine.dao.dto.PerformanceExclusionRow;
import com.wallet.engine.dao.dto.ReconciliationBenefitSumRow;
import com.wallet.engine.dao.dto.ReconciliationTransactionRow;
import com.wallet.engine.dao.dto.ReconciliationUsageRow;
import com.wallet.engine.model.CalcMethod;
import com.wallet.engine.model.PerformanceExclusion;
import com.wallet.engine.model.PerformanceTransaction;
import com.wallet.engine.model.ReconciliationInput;
import com.wallet.engine.model.ReconciliationInput.SourceBenefitUsage;
import com.wallet.engine.model.ReconciliationInput.SourceCardMonth;
import com.wallet.engine.model.ReconciliationInput.StoredBenefitUsage;
import com.wallet.engine.model.ReconciliationInput.StoredCardMonth;
import com.wallet.engine.model.ReconciliationReport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 상태 대사 서비스 — 정산이 증분으로 쌓아 온 상태가 소비내역과 맞는지 기간 단위로 검증한다.
 *
 * 정산은 결제 때 더하고 취소 때 뺄 뿐 전체를 다시 합산하지 않는다. 그래서 한 번 어긋난 값은
 * 오류 없이 남아 한도·실적 판정을 계속 틀리게 만든다. 이 서비스는 같은 기간의 소비내역에서
 * 값을 다시 구해 저장값과 맞춰 본다. <b>읽기만 한다</b> — 어긋난 값을 고치지 않는다.
 * 어느 쪽이 틀렸는지는 원인을 봐야 정해지기 때문이다(상태가 틀렸을 수도, 소비내역이 틀렸을 수도 있다).
 *
 * <pre>
 * 저장된 상태 조회(카드 월 상태 · 혜택 소진)
 *   → 원본 조회(혜택별 할인액 합 · 실적 판정용 거래)
 *   → 거래를 (보유카드, 월)로 묶어 실적인정액 재계산
 *   → 비교(StateReconciler) → 불일치·대조 불가 목록
 * </pre>
 *
 * 실적 재계산에는 정산과 같은 계산기(PerformanceAmountCalculator)를 쓴다. 검증하려는 것이
 * 실적 규칙 자체가 아니라 "하나씩 더해 온 값이 한꺼번에 합산한 값과 같은가"이기 때문이다.
 * 규칙을 따로 구현하면 규칙이 바뀔 때 대사만 옛 규칙으로 남아 없는 불일치를 보고한다.
 *
 * <b>맞춰 보지 않는 값</b>
 * <ul>
 *   <li>스탬프(COUNT_STEP) 혜택의 횟수 — 지급되지 않은 결제의 스탬프는 소비내역에 흔적이 없다.
 *       불일치가 아니라 대조 불가로 따로 보고한다</li>
 *   <li>일 소진(daily_used_amount·daily_used_count·last_applied_date) — 취소가 되돌리지 않는 값이라
 *       유효한 소비내역의 합과 같아야 할 이유가 없고, 당일이 지나면 판정에 쓰이지도 않는다</li>
 * </ul>
 *
 * 조회는 다섯 번이며 회원·카드 수와 무관하다. 공개 API가 아니라 운영·점검 도구가 호출한다.
 */
@Service
public class StateReconciliationService {

    private final ReconciliationMapper reconciliationMapper;
    private final PerformanceMapper performanceMapper;
    private final PerformanceInputAssembler performanceInputAssembler;

    // 순수 계산기는 스테이트리스라 빈으로 두지 않는다 — 프레임워크에서 떼어 둔 설계를 유지한다
    private final PerformanceAmountCalculator performanceAmountCalculator = new PerformanceAmountCalculator();
    private final StateReconciler stateReconciler = new StateReconciler();

    public StateReconciliationService(ReconciliationMapper reconciliationMapper,
                                      PerformanceMapper performanceMapper,
                                      PerformanceInputAssembler performanceInputAssembler) {
        this.reconciliationMapper = reconciliationMapper;
        this.performanceMapper = performanceMapper;
        this.performanceInputAssembler = performanceInputAssembler;
    }

    /**
     * 연월 구간의 상태를 소비내역과 대조한다.
     *
     * 읽기 전용 트랜잭션으로 묶는 이유는 다섯 조회가 <b>같은 시점</b>을 봐야 해서다. 묶지 않으면
     * 상태를 읽고 소비내역을 읽는 사이에 들어온 결제가 한쪽에만 잡혀, 멀쩡한 상태가 불일치로 나온다.
     *
     * @param fromMonth 대조 시작 연월 (포함)
     * @param toMonth   대조 끝 연월 (포함)
     * @param memberId  이 회원의 보유카드로 좁힌다. null이면 전 회원
     */
    @Transactional(readOnly = true)
    public ReconciliationReport reconcile(YearMonth fromMonth, YearMonth toMonth, Long memberId) {
        if (fromMonth == null || toMonth == null) {
            throw new IllegalArgumentException("대조 구간의 시작·끝 연월은 필수다");
        }
        if (fromMonth.isAfter(toMonth)) {
            throw new IllegalArgumentException(
                    "대조 구간의 시작이 끝보다 늦다: %s ~ %s".formatted(fromMonth, toMonth));
        }
        String fromYearMonth = fromMonth.toString();
        String toYearMonth = toMonth.toString();
        LocalDateTime fromInclusive = fromMonth.atDay(1).atStartOfDay();
        LocalDateTime toExclusive = toMonth.plusMonths(1).atDay(1).atStartOfDay();

        // 상태만 한 달 앞에서 읽는다 — 시작 월의 전월실적은 그 직전 월 행의 당월 누적과 맞춰 봐야 한다
        List<CardMonthlyStateRow> stateRows = reconciliationMapper.findStoredStates(
                fromMonth.minusMonths(1).toString(), toYearMonth, memberId);
        List<ReconciliationUsageRow> usageRows = reconciliationMapper.findStoredUsages(
                fromYearMonth, toYearMonth, memberId);
        List<ReconciliationBenefitSumRow> benefitSumRows = reconciliationMapper.findAppliedBenefitSums(
                fromInclusive, toExclusive, memberId);
        List<ReconciliationTransactionRow> transactionRows = reconciliationMapper.findTransactions(
                fromInclusive, toExclusive, memberId);

        return stateReconciler.reconcile(new ReconciliationInput(
                fromYearMonth,
                stateRows.stream().map(this::toStoredCardMonth).toList(),
                usageRows.stream().map(this::toStoredBenefitUsage).toList(),
                recalculatePerformance(transactionRows),
                benefitSumRows.stream().map(this::toSourceBenefitUsage).toList()));
    }

    /**
     * 거래를 (보유카드, 월)로 묶어 실적인정액을 다시 구한다.
     *
     * 정산은 결제마다 한 건씩 판정해 더했다. 여기서는 그 달 거래를 한꺼번에 넣는다 —
     * 판정이 거래 자체 속성에만 달려 있어 두 값은 같아야 하고, 다르면 그것이 찾으려는 불일치다.
     */
    private List<SourceCardMonth> recalculatePerformance(List<ReconciliationTransactionRow> transactionRows) {
        if (transactionRows.isEmpty()) {
            return List.of();
        }
        Map<Long, List<PerformanceExclusion>> exclusionsByCardId = loadExclusionsByCardId(transactionRows);

        // 묶음 순서를 조회 순서대로 유지한다 — 결과 순서가 실행마다 달라지지 않게 한다
        Map<CardMonth, List<ReconciliationTransactionRow>> rowsByCardMonth = transactionRows.stream()
                .collect(Collectors.groupingBy(
                        row -> new CardMonth(row.getUserCardId(), row.getCardId(), row.getBaseYearMonth()),
                        LinkedHashMap::new, Collectors.toList()));

        List<SourceCardMonth> sourceCardMonths = new ArrayList<>();
        rowsByCardMonth.forEach((cardMonth, rows) -> {
            List<PerformanceTransaction> transactions = rows.stream()
                    .map(performanceInputAssembler::toTransaction)
                    .toList();
            long performanceAmount = performanceAmountCalculator.calculate(
                    transactions, exclusionsByCardId.getOrDefault(cardMonth.cardId(), List.of())).amount();
            sourceCardMonths.add(new SourceCardMonth(
                    cardMonth.userCardId(), cardMonth.baseYearMonth(), rows.size(), performanceAmount));
        });
        return sourceCardMonths;
    }

    /** 거래에 등장한 카드의 실적 제외 규칙을 한 번에 읽는다. 카드 수만큼 조회하지 않는다 */
    private Map<Long, List<PerformanceExclusion>> loadExclusionsByCardId(
            List<ReconciliationTransactionRow> transactionRows) {
        List<Long> cardIds = transactionRows.stream()
                .map(ReconciliationTransactionRow::getCardId)
                .distinct()
                .toList();
        return performanceMapper.findExclusionsByCardIds(cardIds).stream()
                .collect(Collectors.groupingBy(
                        PerformanceExclusionRow::getCardId,
                        Collectors.mapping(performanceInputAssembler::toExclusion, Collectors.toList())));
    }

    private StoredCardMonth toStoredCardMonth(CardMonthlyStateRow row) {
        return new StoredCardMonth(
                row.getUserCardId(), row.getBaseYearMonth(), row.getPrevPerformanceAmount(),
                row.getCurrentPerformanceAmount(), row.getSharedLimitUsed());
    }

    private StoredBenefitUsage toStoredBenefitUsage(ReconciliationUsageRow row) {
        return new StoredBenefitUsage(
                row.getUserCardId(), row.getBenefitId(), row.getBaseYearMonth(),
                row.getUsedAmount(), row.getUsedCount(), isStampBenefit(row.getCalcMethod()));
    }

    private SourceBenefitUsage toSourceBenefitUsage(ReconciliationBenefitSumRow row) {
        return new SourceBenefitUsage(
                row.getUserCardId(), row.getBenefitId(), row.getBaseYearMonth(),
                row.getDiscountAmount(), row.getAppliedCount(),
                "Y".equals(row.getUseSharedLimit()), isStampBenefit(row.getCalcMethod()));
    }

    private boolean isStampBenefit(String calcMethod) {
        return CalcMethod.COUNT_STEP.name().equals(calcMethod);
    }

    /** 실적 재계산의 묶음 키. 카드 마스터 ID는 보유카드에 종속이라 묶음을 쪼개지 않는다 */
    private record CardMonth(long userCardId, long cardId, String baseYearMonth) {
    }
}
