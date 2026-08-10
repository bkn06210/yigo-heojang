package com.wallet.engine.service;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.engine.assembler.BenefitCandidateAssembler;
import com.wallet.engine.assembler.CardStateAssembler;
import com.wallet.engine.assembler.PerformanceInputAssembler;
import com.wallet.engine.calculator.CardBenefitSelector;
import com.wallet.engine.calculator.PerformanceAmountCalculator;
import com.wallet.engine.calculator.PerformanceTierResolver;
import com.wallet.engine.dao.BenefitMapper;
import com.wallet.engine.dao.PaymentTargetMapper;
import com.wallet.engine.dao.PerformanceMapper;
import com.wallet.engine.dao.SettlementMapper;
import com.wallet.engine.dao.dto.BenefitPeriodUsageRow;
import com.wallet.engine.dao.dto.BenefitRow;
import com.wallet.engine.dao.dto.BenefitUsageRow;
import com.wallet.engine.dao.dto.ExpenseRow;
import com.wallet.engine.dao.dto.MonthlyStatusRow;
import com.wallet.engine.dao.dto.OptionSelectionRow;
import com.wallet.engine.dao.dto.PaymentTargetRow;
import com.wallet.engine.dao.dto.PerformanceTierRow;
import com.wallet.engine.dto.CardMonthlyStatus;
import com.wallet.engine.dto.PaymentSettlementResult;
import com.wallet.engine.dto.SettlementCommand;
import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.CardBenefitSelection;
import com.wallet.engine.model.CardState;
import com.wallet.engine.model.PaymentRequest;
import com.wallet.engine.model.PaymentTarget;
import com.wallet.engine.model.PerformanceExclusion;
import com.wallet.engine.model.PerformancePeriod;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.PerformanceTier;
import com.wallet.engine.model.PerformanceTransaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 정산 서비스 — 결제로 늘어난 소진을 취소가 되돌린다(역산).
 *
 * 추천·현황 조회가 상태 테이블을 읽기만 하는 것과 달리, 이 서비스는 <b>쓴다</b>.
 * 취소는 <b>재계산이 아니라 역산</b>이다 — 취소된 소비내역이 결제 때 더했던 값을 그대로 빼며,
 * 다른 거래의 적용혜택·할인액은 건드리지 않는다(소급 재계산 금지, 실제 카드사 동작과 동일).
 *
 * <pre>
 * 소비내역 조회(+소유권)          없거나 남의 것 → 404
 *   → 이미 취소면                                → 409
 *   → 당월 아니면(전월 이전)                     → 범위 밖(400)
 *   → APPROVED→CANCELED 원자적 전환(compare-and-set)   0이면 이미 취소(경합) → 409
 *   → 기여분 역산 차감(실적 −, 통합한도 −, 혜택별 −, 횟수 −)
 *   → 차감 반영된 현황(CardMonthlyStatus) 반환
 * </pre>
 */
@Service
public class SettlementService {

    private final SettlementMapper settlementMapper;
    private final PerformanceMapper performanceMapper;
    private final BenefitMapper benefitMapper;
    private final PaymentTargetMapper paymentTargetMapper;
    private final PerformanceInputAssembler performanceInputAssembler;
    private final BenefitCandidateAssembler benefitCandidateAssembler;
    private final CardStateAssembler cardStateAssembler;
    private final CardMonthlyStatusBuilder statusBuilder;

    // 순수 계산기는 스테이트리스라 빈으로 두지 않는다 — 프레임워크에서 떼어 둔 설계를 유지한다
    private final PerformanceAmountCalculator performanceAmountCalculator = new PerformanceAmountCalculator();
    private final PerformanceTierResolver tierResolver = new PerformanceTierResolver();
    private final CardBenefitSelector cardBenefitSelector = new CardBenefitSelector();

    public SettlementService(SettlementMapper settlementMapper,
                             PerformanceMapper performanceMapper,
                             BenefitMapper benefitMapper,
                             PaymentTargetMapper paymentTargetMapper,
                             PerformanceInputAssembler performanceInputAssembler,
                             BenefitCandidateAssembler benefitCandidateAssembler,
                             CardStateAssembler cardStateAssembler,
                             CardMonthlyStatusBuilder statusBuilder) {
        this.settlementMapper = settlementMapper;
        this.performanceMapper = performanceMapper;
        this.benefitMapper = benefitMapper;
        this.paymentTargetMapper = paymentTargetMapper;
        this.performanceInputAssembler = performanceInputAssembler;
        this.benefitCandidateAssembler = benefitCandidateAssembler;
        this.cardStateAssembler = cardStateAssembler;
        this.statusBuilder = statusBuilder;
    }

    /**
     * 결제 성공 시 혜택을 계산해 상태를 가산하고, 소비내역에 기록할 혜택 결과를 돌려준다.
     *
     * 소비내역 도메인이 결제 트랜잭션 안에서 직접 호출한다(REST 아님) — "결제됐는데 혜택 미기록"
     * 상태가 존재할 수 없도록 같은 트랜잭션에 묶인다. 계산 규칙은 추천과 동일한 계산기를 재사용하며,
     * 금액이 확정값이라 {@code amountEstimated=false}로 넣는다.
     *
     * <pre>
     * 실적 판정(전월실적 → 구간)
     *   → 대상·혜택·현재 소진 로드 → 혜택 계산(최적 1개)
     *   → 상태 가산: 당월누적실적 +, 통합한도사용 +(적용 혜택이 통합한도 사용 시), 혜택별 사용액·횟수 +
     *      (기준월 행이 없으면 만들며 prev를 이월 = 월 롤오버)
     *   → applied_benefit_id·discount_amount 반환
     * </pre>
     */
    @Transactional
    public PaymentSettlementResult applyPayment(SettlementCommand command) {
        YearMonth month = YearMonth.from(command.paymentDate());
        String baseYearMonth = month.toString();
        String previousYearMonth = month.minusMonths(1).toString();
        LocalDate appliedDate = command.paymentDate().toLocalDate();

        // 실적 판정 — 저장된 전월실적으로 구간을 정한다(월 롤오버 전이면 직전월 누적액을 이월)
        long prevPerformanceAmount = resolvePrevPerformance(
                command.userCardId(), baseYearMonth, previousYearMonth);
        List<PerformanceTierRow> tierRows = performanceMapper.findTiers(command.cardId());
        PerformanceStatus status = tierResolver.judge(
                performanceInputAssembler.toTiers(tierRows, PerformancePeriod.MONTH), prevPerformanceAmount);
        PerformanceStatus quarterStatus = judgeQuarter(command.userCardId(), tierRows, month);

        // 대상·혜택·현재 소진 로드 → 혜택 계산(확정 금액)
        PaymentTarget target = resolveTarget(command);
        List<BenefitRow> benefitRows = benefitMapper.findActiveBenefits(
                command.cardId(), status.tierId(), quarterStatus == null ? null : quarterStatus.tierId());
        List<BenefitCandidate> candidates = benefitCandidateAssembler.toCandidates(
                benefitRows, benefitMapper.findExclusions(command.cardId()));
        CardState cardState = loadCardState(
                command.userCardId(), baseYearMonth, status, quarterStatus, appliedDate);
        PaymentRequest request = PaymentRequest.confirmed(target, command.amount(), command.paymentType());
        CardBenefitSelection selection = cardBenefitSelector.select(candidates, cardState, request);

        long discount = selection.benefitAmount();
        // 혜택이 뽑혔어도 한도 소진으로 0원이면 실제로 받은 혜택이 없다 —
        // 소진(횟수·금액)을 올리지 않고 기록도 남기지 않는다(횟수 헛소비·유령 혜택 기록 방지).
        Long appliedBenefitId = (selection.hasBenefit() && discount > 0) ? selection.benefitId() : null;

        // 상태 가산 — 실적은 이 거래의 인정분, 통합한도는 적용 혜택이 통합한도를 쓸 때만
        long performanceContribution = performanceContribution(
                command.cardId(), command.amount(), target.getCategoryCode(), target.getParentCategoryCode(),
                command.paymentType(), command.interestFree(), discount,
                excludesFromPerformance(benefitRows, appliedBenefitId));
        long sharedLimitContribution = usesSharedLimit(benefitRows, appliedBenefitId) ? discount : 0L;
        settlementMapper.upsertMonthlyStateAdd(command.userCardId(), baseYearMonth,
                prevPerformanceAmount, performanceContribution, sharedLimitContribution);

        addBenefitUsages(command.userCardId(), baseYearMonth, appliedDate, selection, appliedBenefitId, discount);
        return new PaymentSettlementResult(appliedBenefitId, discount);
    }

    /**
     * 혜택별 소진을 누적한다. 실제 혜택을 받았을 때(금액 &gt; 0)와 스탬프가 찍혔을 때가 서로 다른 조건이다.
     *
     * <pre>
     * 혜택액 &gt; 0   금액·횟수 누적. 0원이면 실제로 받은 혜택이 없어 누적하지 않는다
     *              (횟수 헛소비·유령 혜택 기록 방지)
     * 스탬프       금액이 0원이어도 횟수를 올린다. 스탬프는 그 결제에서 다른 혜택을 받았는지와
     *              무관하게 찍히며, 안 올리면 N회째가 영영 오지 않아 지급 자체가 사라진다
     * </pre>
     *
     * 적용된 혜택이 스탬프이기도 하면 한 번만 올린다 — 두 경로로 각각 올리면 두 배로 세어진다.
     */
    private void addBenefitUsages(long userCardId, String baseYearMonth, LocalDate appliedDate,
                                  CardBenefitSelection selection, Long appliedBenefitId, long discount) {
        for (Long stampedBenefitId : selection.stampedBenefitIds()) {
            boolean paidThisTime = stampedBenefitId.equals(appliedBenefitId);
            settlementMapper.upsertBenefitUsageAdd(userCardId, stampedBenefitId, baseYearMonth,
                    paidThisTime ? discount : 0L, appliedDate);
        }
        if (appliedBenefitId != null && !selection.stampedBenefitIds().contains(appliedBenefitId)) {
            settlementMapper.upsertBenefitUsageAdd(userCardId, appliedBenefitId, baseYearMonth,
                    discount, appliedDate);
        }
    }

    /**
     * 취소된 소비내역 한 건의 기여분을 상태에서 역산 차감하고, 차감 반영된 카드 현황을 돌려준다.
     *
     * @param memberId  회원 ID (토큰에서 추출 — 소유권 필터)
     * @param expenseId 취소된 소비내역 ID
     * @param today     기준일. 당월(취소 가능 범위) 판정에 쓴다
     */
    @Transactional
    public CardMonthlyStatus cancelPayment(long memberId, long expenseId, LocalDate today) {
        ExpenseRow expense = settlementMapper.findExpenseForSettlement(expenseId, memberId);
        if (expense == null) {
            // 없거나 남의 소비내역 — 존재를 노출하지 않으려 소유권 불일치도 404로 통일한다
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if ("CANCELED".equals(expense.getPaymentStatus())) {
            throw new BusinessException(ErrorCode.ALREADY_CANCELED);
        }

        YearMonth expenseMonth = YearMonth.from(expense.getPaymentDate());
        if (!expenseMonth.equals(YearMonth.from(today))) {
            // 전월 이전 거래 취소는 범위 밖 — 전월실적이 바뀌면 구간·통합한도 전체 재판정이 필요하다
            throw new BusinessException(ErrorCode.RESOURCE_STATE_INVALID);
        }
        String baseYearMonth = expenseMonth.toString();

        // APPROVED일 때만 CANCELED로 전환. 0이면 조회 이후 사이에 이미 취소된 것(경합) → 중복 차감 차단
        if (settlementMapper.markExpenseCanceled(expenseId) == 0) {
            throw new BusinessException(ErrorCode.ALREADY_CANCELED);
        }
        settlementMapper.markPaymentCanceledByExpense(expenseId);

        deductContributions(expense, baseYearMonth);

        return buildStatus(expense.getUserCardId(), expense.getCardId(), baseYearMonth);
    }

    /**
     * 취소 건이 결제 때 더한 값을 역산해 상태에서 뺀다. 전부 음수 델타로 넘기며,
     * 매퍼가 {@code GREATEST(0, …)}로 하한 0을 보장한다.
     */
    private void deductContributions(ExpenseRow expense, String baseYearMonth) {
        long performanceContribution = performanceContributionOf(expense);
        // 통합한도는 적용 혜택이 통합한도를 쓴 경우에만 소진됐다 — 그 경우에만 되돌린다
        long sharedLimitContribution = "Y".equals(expense.getUseSharedLimit())
                ? expense.getDiscountAmount() : 0L;

        settlementMapper.applyStateDelta(
                expense.getUserCardId(), baseYearMonth,
                -performanceContribution, -sharedLimitContribution);

        List<Long> stampedBenefitIds = stampedBenefitIdsOf(expense, baseYearMonth);
        for (Long stampedBenefitId : stampedBenefitIds) {
            boolean paidThisTime = stampedBenefitId.equals(expense.getAppliedBenefitId());
            settlementMapper.applyUsageDelta(expense.getUserCardId(), stampedBenefitId, baseYearMonth,
                    paidThisTime ? -expense.getDiscountAmount() : 0L, -1);
        }

        // 혜택 미적용 거래(applied_benefit_id=NULL)면 되돌릴 소진이 없다
        Long appliedBenefitId = expense.getAppliedBenefitId();
        if (appliedBenefitId != null && !stampedBenefitIds.contains(appliedBenefitId)) {
            settlementMapper.applyUsageDelta(
                    expense.getUserCardId(), appliedBenefitId, baseYearMonth,
                    -expense.getDiscountAmount(), -1);
        }
    }

    /**
     * 취소 건이 찍었던 스탬프(COUNT_STEP)를 가려낸다.
     *
     * expense에는 적용된 혜택 하나만 남는다. 스탬프는 다른 혜택이 선택된 결제에서도 찍히므로,
     * 저장된 값만 보고 되돌리면 진행 횟수가 영구히 부풀어 다음 지급이 앞당겨진다.
     *
     * 그래서 거래 속성으로 다시 판정한다 — 실적 기여분을 단건 재판정하는 것과 같은 방식이다.
     * 소진을 0으로 둔 상태로 계산하는 이유는 지금 소진에 이 거래의 몫이 이미 들어 있어서다.
     * 그 값 그대로 판정하면 자기 자신 때문에 횟수 한도에 걸려 되돌릴 대상에서 빠진다.
     *
     * 한계: 결제 당시 횟수 한도에 걸려 찍히지 않았던 스탬프도 되돌림 대상이 된다.
     * 차감은 {@code GREATEST(0, …)}로 막혀 음수가 되지는 않는다.
     */
    private List<Long> stampedBenefitIdsOf(ExpenseRow expense, String baseYearMonth) {
        YearMonth baseMonth = YearMonth.parse(baseYearMonth);
        List<PerformanceTierRow> tierRows = performanceMapper.findTiers(expense.getCardId());
        PerformanceStatus status = tierResolver.judge(
                performanceInputAssembler.toTiers(tierRows, PerformancePeriod.MONTH),
                resolvePrevPerformance(expense.getUserCardId(), baseYearMonth,
                        baseMonth.minusMonths(1).toString()));
        PerformanceStatus quarterStatus = judgeQuarter(expense.getUserCardId(), tierRows, baseMonth);

        List<BenefitCandidate> candidates = benefitCandidateAssembler.toCandidates(
                benefitMapper.findActiveBenefits(expense.getCardId(), status.tierId(),
                        quarterStatus == null ? null : quarterStatus.tierId()),
                benefitMapper.findExclusions(expense.getCardId()));
        CardState stateWithoutUsage = new CardState(
                status, quarterStatus, 0L, List.of(),
                loadOptionSelections(expense.getUserCardId(), baseYearMonth));

        PaymentRequest request = PaymentRequest.confirmed(
                toPaymentTarget(expense), expense.getAmount(), expense.getPaymentType());
        return cardBenefitSelector.select(candidates, stateWithoutUsage, request).stampedBenefitIds();
    }

    /**
     * 취소 건의 저장된 대상 정보를 매칭용 대상으로 펼친다.
     * 조회 시점에 이미 조인해 온 값이라 대상 조회를 다시 하지 않는다.
     */
    private PaymentTarget toPaymentTarget(ExpenseRow expense) {
        return PaymentTarget.builder()
                .merchantId(expense.getMerchantId())
                .merchantCode(expense.getMerchantCode())
                .categoryId(expense.getCategoryId())
                .categoryCode(expense.getCategoryCode())
                .parentCategoryId(expense.getParentCategoryId())
                .parentCategoryCode(expense.getParentCategoryCode())
                .build();
    }

    /** 취소 건의 실적 기여분 — 저장된 거래 속성으로 단건 판정한다. */
    private long performanceContributionOf(ExpenseRow expense) {
        return performanceContribution(
                expense.getCardId(), expense.getAmount(),
                expense.getCategoryCode(), expense.getParentCategoryCode(),
                expense.getPaymentType(), "Y".equals(expense.getIsInterestFree()),
                expense.getDiscountAmount(),
                "Y".equals(expense.getExcludeFromPerformance()));
    }

    /**
     * 이 거래가 실적에 기여한 인정액을 구한다. 저장된 합계를 맹목 가감하지 않고
     * <b>거래 자체 속성</b>(결제수단·무이자·카테고리·할인여부)으로 단건 판정을 다시 돌린다 —
     * 결정론적이라 다른 거래에 영향받지 않으므로 "소급 재계산 금지" 원칙과 부딪히지 않는다.
     * 실적 제외에 걸린 거래(예: 할인받은 거래)는 실적에 안 잡히므로 0을 돌려준다.
     *
     * 가산·취소가 같은 판정을 공유한다 — 결제 때 더한 인정분과 취소 때 뺄 인정분이 어긋나지 않게 한다.
     * 할인 여부 판정에 쓰는 discountAmount는 가산이면 방금 계산한 혜택액, 취소면 저장된 값이다.
     */
    private long performanceContribution(long cardId, long amount, String categoryCode,
                                         String parentCategoryCode, String paymentType,
                                         boolean interestFree, long discountAmount,
                                         boolean benefitExcludedFromPerformance) {
        PerformanceTransaction transaction = PerformanceTransaction.builder()
                .amount(amount)
                .canceled(false)
                .categoryCode(categoryCode)
                .parentCategoryCode(parentCategoryCode)
                .paymentType(paymentType)
                .interestFree(interestFree)
                .discountAmount(discountAmount)
                .benefitExcludedFromPerformance(benefitExcludedFromPerformance)
                .build();
        List<PerformanceExclusion> exclusions = performanceInputAssembler.toExclusions(
                performanceMapper.findExclusions(cardId));
        return performanceAmountCalculator.calculate(List.of(transaction), exclusions).amount();
    }

    /**
     * 전월실적 취득 — 저장된 집계값을 읽는다(재합산 아님).
     * 기준월 행이 있으면 그 행의 prev, 없으면 직전월 행의 당월누적(이월 전 원본), 둘 다 없으면 0.
     */
    private long resolvePrevPerformance(long userCardId, String baseYearMonth, String previousYearMonth) {
        MonthlyStatusRow baseRow = settlementMapper.findMonthlyStatus(userCardId, baseYearMonth);
        if (baseRow != null) {
            return baseRow.getPrevPerformanceAmount();
        }
        MonthlyStatusRow previousRow = settlementMapper.findMonthlyStatus(userCardId, previousYearMonth);
        return previousRow != null ? previousRow.getCurrentPerformanceAmount() : 0L;
    }

    /** 가산의 계산 입력이 될 카드 상태를 조립한다. 기준월 행이 없으면 소진 0(첫 결제 전)으로 본다. */
    /**
     * 전분기 실적으로 분기 구간을 판정한다. 분기 구간표가 없는 카드면 null.
     * 전월실적과 같이 저장된 집계값(직전 분기 세 달의 실적인정액 합)을 읽는다.
     */
    private PerformanceStatus judgeQuarter(long userCardId, List<PerformanceTierRow> tierRows,
                                           YearMonth baseMonth) {
        List<PerformanceTier> quarterTiers =
                performanceInputAssembler.toTiers(tierRows, PerformancePeriod.QUARTER);
        if (quarterTiers.isEmpty()) {
            return null;
        }
        long quarterAmount = settlementMapper.findPerformanceSumByUserCard(userCardId,
                UsagePeriod.previousQuarterStart(baseMonth).toString(),
                UsagePeriod.previousQuarterEnd(baseMonth).toString());
        return tierResolver.judge(quarterTiers, quarterAmount);
    }

    private CardState loadCardState(long userCardId, String baseYearMonth,
                                    PerformanceStatus status, PerformanceStatus quarterStatus,
                                    LocalDate appliedDate) {
        MonthlyStatusRow baseRow = settlementMapper.findMonthlyStatus(userCardId, baseYearMonth);
        long sharedLimitUsed = baseRow != null ? baseRow.getSharedLimitUsed() : 0L;
        List<BenefitUsageRow> usageRows = settlementMapper.findUsagesByUserCard(userCardId, baseYearMonth);

        // 분기·연 한도는 저장값이 없어 월 소진 행을 그 기간만큼 합산해 얻는다
        YearMonth baseMonth = YearMonth.parse(baseYearMonth);
        List<BenefitPeriodUsageRow> quarterUsageRows = settlementMapper.findPeriodUsagesByUserCard(
                userCardId, UsagePeriod.quarterStart(baseMonth).toString(), baseYearMonth);
        List<BenefitPeriodUsageRow> yearUsageRows = settlementMapper.findPeriodUsagesByUserCard(
                userCardId, UsagePeriod.yearStart(baseMonth).toString(), baseYearMonth);

        return new CardState(
                status, quarterStatus, sharedLimitUsed,
                cardStateAssembler.toUsages(usageRows, quarterUsageRows, yearUsageRows, appliedDate),
                loadOptionSelections(userCardId, baseYearMonth));
    }

    private Map<String, String> loadOptionSelections(long userCardId, String baseYearMonth) {
        return settlementMapper.findOptionSelectionsByUserCard(userCardId, baseYearMonth).stream()
                .collect(Collectors.toMap(
                        OptionSelectionRow::getOptionGroupCode, OptionSelectionRow::getSelectedOptionKey));
    }

    /**
     * 요청의 merchantId·categoryId를 매칭에 쓸 대상으로 펼친다.
     * 소비내역의 category_id는 NOT NULL이고 merchant_id는 FK라, 정상 결제면 조회가 항상 맞는다.
     */
    private PaymentTarget resolveTarget(SettlementCommand command) {
        PaymentTargetRow row = command.merchantId() != null
                ? paymentTargetMapper.findByMerchantId(command.merchantId())
                : paymentTargetMapper.findByCategoryId(command.categoryId());
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return PaymentTarget.builder()
                .merchantId(row.getMerchantId())
                .merchantCode(row.getMerchantCode())
                .categoryId(row.getCategoryId())
                .categoryCode(row.getCategoryCode())
                .parentCategoryId(row.getParentCategoryId())
                .parentCategoryCode(row.getParentCategoryCode())
                .build();
    }

    /**
     * 적용된 혜택이 "이 혜택을 받은 거래는 실적에서 뺀다"고 지정돼 있는지.
     *
     * 재합산 경로(전월실적)는 이 값을 조인해 읽으므로, 가산에서 빼먹으면 같은 거래가
     * 당월 누적에는 잡히고 재합산에는 안 잡혀 두 값이 어긋난다.
     */
    private boolean excludesFromPerformance(List<BenefitRow> benefitRows, Long benefitId) {
        if (benefitId == null) {
            return false;
        }
        return benefitRows.stream()
                .filter(row -> row.getBenefitId() == benefitId)
                .anyMatch(row -> "Y".equals(row.getExcludeFromPerformance()));
    }

    /** 적용된 혜택이 통합할인한도를 쓰는지 — 그 경우에만 통합한도 소진을 가산한다. */
    private boolean usesSharedLimit(List<BenefitRow> benefitRows, Long benefitId) {
        if (benefitId == null) {
            return false;
        }
        return benefitRows.stream()
                .filter(row -> row.getBenefitId() == benefitId)
                .anyMatch(row -> "Y".equals(row.getUseSharedLimit()));
    }

    /**
     * 차감 반영된 카드 현황을 조립한다.
     *
     * 혜택 목록은 판정된 구간(tierId)의 개별한도를 반영해야 하므로, 전월실적으로 구간을 판정한 뒤
     * 그 구간 기준으로 활성 혜택을 조회한다. 조립(진행률·묶음 한도 합산)은 재사용 빌더가 한다.
     */
    private CardMonthlyStatus buildStatus(long userCardId, long cardId, String baseYearMonth) {
        MonthlyStatusRow statusRow = settlementMapper.findMonthlyStatus(userCardId, baseYearMonth);
        if (statusRow == null) {
            // 당월 거래를 취소했는데 상태 행이 없다 = 데이터 불일치(가산이 행을 만들었어야 함)
            throw new BusinessException(ErrorCode.SERVER_INTERNAL_ERROR);
        }
        List<PerformanceTierRow> tierRows = performanceMapper.findTiers(cardId);
        List<PerformanceTier> tiers = performanceInputAssembler.toTiers(tierRows, PerformancePeriod.MONTH);
        long tierId = tierResolver.judge(tiers, statusRow.getPrevPerformanceAmount()).tierId();
        PerformanceStatus quarterStatus =
                judgeQuarter(userCardId, tierRows, YearMonth.parse(baseYearMonth));
        List<BenefitRow> benefitRows = benefitMapper.findActiveBenefits(
                cardId, tierId, quarterStatus == null ? null : quarterStatus.tierId());
        Map<Long, Long> usedAmountByBenefit = settlementMapper
                .findUsagesByUserCard(userCardId, baseYearMonth).stream()
                .collect(Collectors.toMap(BenefitUsageRow::getBenefitId, BenefitUsageRow::getUsedAmount));

        return statusBuilder.build(
                statusRow.getUserCardId(), statusRow.getCardName(), baseYearMonth,
                statusRow.getPrevPerformanceAmount(), statusRow.getCurrentPerformanceAmount(),
                statusRow.getSharedLimitUsed(), tiers, benefitRows, usedAmountByBenefit,
                loadOptionSelections(userCardId, baseYearMonth));
    }
}
