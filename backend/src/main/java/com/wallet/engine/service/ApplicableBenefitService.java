package com.wallet.engine.service;

import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.engine.assembler.BenefitCandidateAssembler;
import com.wallet.engine.assembler.CardStateAssembler;
import com.wallet.engine.assembler.PerformanceInputAssembler;
import com.wallet.engine.calculator.BenefitMatcher;
import com.wallet.engine.calculator.PerformanceTierResolver;
import com.wallet.engine.dao.BenefitMapper;
import com.wallet.engine.dao.CardStateMapper;
import com.wallet.engine.dao.PaymentTargetMapper;
import com.wallet.engine.dao.PerformanceMapper;
import com.wallet.engine.dao.UserCardMapper;
import com.wallet.engine.dao.dto.BenefitRow;
import com.wallet.engine.dao.dto.CardMonthlyStateRow;
import com.wallet.engine.dao.dto.CardPerformanceSumRow;
import com.wallet.engine.dao.dto.PaymentTargetRow;
import com.wallet.engine.dao.dto.PerformanceTierRow;
import com.wallet.engine.dao.dto.UserCardRow;
import com.wallet.engine.dto.ApplicableBenefitCard;
import com.wallet.engine.dto.ApplicableBenefitItem;
import com.wallet.engine.dto.ApplicableBenefitResponse;
import com.wallet.engine.model.BenefitCandidate;
import com.wallet.engine.model.BenefitRule;
import com.wallet.engine.model.PaymentRequest;
import com.wallet.engine.model.PaymentTarget;
import com.wallet.engine.model.PerformancePeriod;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.PerformanceTier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 특정 가맹점·업종에서 보유 카드가 갖는 혜택 조회.
 *
 * <p>결제 직전 추천({@link RecommendationService})과 답하는 질문이 다르다.
 * 추천은 "지금 8,000원 결제하면 어느 카드가 얼마 유리한가"이고, 여기는
 * "이 가맹점에 걸린 혜택이 무엇이고 어떤 조건인가"다.
 * 앞은 금액이 있어야 성립하지만 뒤는 금액 없이 답할 수 있다.
 *
 * <p>그래서 혜택액을 계산하지 않는다. 대신 매칭된 혜택을 조건과 함께 전부 내려준다.
 * 실적을 못 채워 지금은 못 받는 혜택도 사유와 함께 담는다 — "채우면 받을 수 있다"가
 * "혜택이 없다"보다 쓸모 있는 정보다.
 *
 * <p>한도 소진은 담지 않는다. 소진 현황은 보유 카드 현황 조회가 이미 내려주고,
 * 여기서 또 계산하면 같은 값이 두 경로로 나가 어긋날 여지가 생긴다.
 */
@Service
public class ApplicableBenefitService {

    private static final String REASON_PERFORMANCE_NOT_MET = "전월 실적 미달";

    /**
     * 매칭 판정에만 쓰는 명목 금액.
     *
     * 대상 일치와 제외 규칙은 금액을 보지 않는다. 다만 PaymentRequest가 0 이하를 거부해
     * 자리를 채워야 한다. 이 값은 응답 어디에도 쓰이지 않는다.
     */
    private static final long NOMINAL_AMOUNT_FOR_MATCHING = 1L;

    private final UserCardMapper userCardMapper;
    private final CardStateMapper cardStateMapper;
    private final BenefitMapper benefitMapper;
    private final PerformanceMapper performanceMapper;
    private final PaymentTargetMapper paymentTargetMapper;
    private final CardStateAssembler cardStateAssembler;
    private final BenefitCandidateAssembler benefitCandidateAssembler;
    private final PerformanceInputAssembler performanceInputAssembler;

    private final PerformanceTierResolver tierResolver = new PerformanceTierResolver();
    private final BenefitMatcher benefitMatcher = new BenefitMatcher();

    public ApplicableBenefitService(UserCardMapper userCardMapper,
                                    CardStateMapper cardStateMapper,
                                    BenefitMapper benefitMapper,
                                    PerformanceMapper performanceMapper,
                                    PaymentTargetMapper paymentTargetMapper,
                                    CardStateAssembler cardStateAssembler,
                                    BenefitCandidateAssembler benefitCandidateAssembler,
                                    PerformanceInputAssembler performanceInputAssembler) {
        this.userCardMapper = userCardMapper;
        this.cardStateMapper = cardStateMapper;
        this.benefitMapper = benefitMapper;
        this.performanceMapper = performanceMapper;
        this.paymentTargetMapper = paymentTargetMapper;
        this.cardStateAssembler = cardStateAssembler;
        this.benefitCandidateAssembler = benefitCandidateAssembler;
        this.performanceInputAssembler = performanceInputAssembler;
    }

    /**
     * @param memberId   회원 ID (토큰에서 추출한 값)
     * @param merchantId 가맹점 ID. null이면 categoryId를 본다
     * @param categoryId 업종 ID. 둘 다 null이면 전 가맹점(ALL) 혜택만 매칭된다
     * @param today      기준일. 기준월 판정에 쓴다
     */
    @Transactional(readOnly = true)
    public ApplicableBenefitResponse lookup(long memberId, Long merchantId, Long categoryId,
                                            LocalDate today) {
        List<UserCardRow> cards = userCardMapper.findActiveCards(memberId);
        if (cards.isEmpty()) {
            return new ApplicableBenefitResponse(List.of());
        }

        PaymentRequest matchRequest = PaymentRequest.estimated(
                resolvePaymentTarget(merchantId, categoryId), NOMINAL_AMOUNT_FOR_MATCHING, null);

        YearMonth baseMonth = YearMonth.from(today);
        String baseYearMonth = baseMonth.toString();
        String previousYearMonth = baseMonth.minusMonths(1).toString();

        Map<Long, List<CardMonthlyStateRow>> statesByUserCard = cardStateMapper
                .findStates(memberId, baseYearMonth, previousYearMonth).stream()
                .collect(Collectors.groupingBy(CardMonthlyStateRow::getUserCardId));
        Map<Long, Long> quarterPerformanceByUserCard = cardStateMapper
                .findPerformanceSums(memberId,
                        UsagePeriod.previousQuarterStart(baseMonth).toString(),
                        UsagePeriod.previousQuarterEnd(baseMonth).toString()).stream()
                .collect(Collectors.toMap(
                        CardPerformanceSumRow::getUserCardId, CardPerformanceSumRow::getPerformanceAmount));
        Map<Long, List<PerformanceTierRow>> tiersByCard = performanceMapper
                .findTiersByCardIds(cards.stream().map(UserCardRow::getCardId).distinct().toList()).stream()
                .collect(Collectors.groupingBy(PerformanceTierRow::getCardId));

        List<ApplicableBenefitCard> items = new ArrayList<>();
        for (UserCardRow card : cards) {
            items.add(evaluateCard(card, matchRequest, baseYearMonth, previousYearMonth,
                    statesByUserCard.getOrDefault(card.getUserCardId(), List.of()),
                    quarterPerformanceByUserCard.getOrDefault(card.getUserCardId(), 0L),
                    tiersByCard.getOrDefault(card.getCardId(), List.of())));
        }
        return new ApplicableBenefitResponse(items);
    }

    private ApplicableBenefitCard evaluateCard(UserCardRow card, PaymentRequest matchRequest,
                                               String baseYearMonth, String previousYearMonth,
                                               List<CardMonthlyStateRow> stateRows,
                                               long quarterPerformanceAmount,
                                               List<PerformanceTierRow> tierRows) {
        long prevPerformanceAmount = cardStateAssembler.resolvePrevPerformanceAmount(
                stateRows, baseYearMonth, previousYearMonth);

        List<PerformanceTier> monthTiers =
                performanceInputAssembler.toTiers(tierRows, PerformancePeriod.MONTH);
        PerformanceStatus status = tierResolver.judge(monthTiers, prevPerformanceAmount);

        // 한 카드가 전월 축과 전분기 축 구간표를 함께 가질 수 있다. 하나로 조회하면
        // 분기 혜택이 월 구간의 개별한도를 쓰게 된다(에러 없이 값만 틀린다).
        List<PerformanceTier> quarterTiers =
                performanceInputAssembler.toTiers(tierRows, PerformancePeriod.QUARTER);
        PerformanceStatus quarterStatus = quarterTiers.isEmpty()
                ? null : tierResolver.judge(quarterTiers, quarterPerformanceAmount);

        List<BenefitRow> benefitRows = benefitMapper.findActiveBenefits(
                card.getCardId(), status.tierId(), quarterStatus == null ? null : quarterStatus.tierId());
        Map<Long, BenefitRow> rowsById = benefitRows.stream()
                .collect(Collectors.toMap(BenefitRow::getBenefitId, row -> row, (a, b) -> a));

        List<ApplicableBenefitItem> benefits = benefitCandidateAssembler
                .toCandidates(benefitRows, benefitMapper.findExclusions(card.getCardId())).stream()
                .filter(candidate -> benefitMatcher.matches(candidate, matchRequest))
                .map(candidate -> toItem(candidate, rowsById.get(candidate.getBenefitId()),
                        status, quarterStatus))
                .filter(item -> item != null)
                .toList();

        return new ApplicableBenefitCard(card.getUserCardId(), card.getCardName(),
                prevPerformanceAmount, requiredPerformanceAmount(monthTiers),
                status.performanceMet(), benefits);
    }

    private ApplicableBenefitItem toItem(BenefitCandidate candidate, BenefitRow row,
                                         PerformanceStatus monthStatus, PerformanceStatus quarterStatus) {
        if (row == null) {
            return null;
        }

        // 금액·한도는 원본 행이 아니라 규칙에서 읽는다. 판정된 실적구간의 개별한도가
        // 이미 반영돼 있어, 행의 기본값을 그대로 쓰면 구간별로 다른 값이 사라진다.
        BenefitRule rule = candidate.getRule();
        boolean requirePerformance = rule.isRequirePerformance();

        // 혜택마다 실적 축이 다르다. 월 실적을 채웠어도 분기 축 혜택은 별도 판정이다.
        PerformanceStatus axis = candidate.getPerformancePeriod() == PerformancePeriod.QUARTER
                ? quarterStatus : monthStatus;
        boolean met = axis != null && axis.performanceMet();
        boolean available = !requirePerformance || met;

        return new ApplicableBenefitItem(
                rule.getBenefitId(), row.getBenefitName(),
                rule.getBenefitKind().name(), rule.getCalcMethod().name(),
                rule.getBenefitValue(), requirePerformance,
                available, available ? null : REASON_PERFORMANCE_NOT_MET,
                rule.getMinTxnAmount(), rule.getMonthlyLimit(), rule.getStepCount(),
                candidate.getOptionGroupCode());
    }

    /**
     * 실적 조건이 걸린 혜택을 받기 위해 필요한 전월실적.
     *
     * 0원 구간은 모든 카드가 갖는 기본 구간이라 제외한다. 그 위 구간이 없으면
     * 채울 실적 자체가 없는 카드이므로 null이다.
     */
    private Long requiredPerformanceAmount(List<PerformanceTier> tiers) {
        return tiers.stream()
                .map(PerformanceTier::minPerformanceAmount)
                .filter(amount -> amount > 0)
                .min(Long::compare)
                .orElse(null);
    }

    private PaymentTarget resolvePaymentTarget(Long merchantId, Long categoryId) {
        if (merchantId != null) {
            return toPaymentTarget(paymentTargetMapper.findByMerchantId(merchantId));
        }
        if (categoryId != null) {
            return toPaymentTarget(paymentTargetMapper.findByCategoryId(categoryId));
        }
        // 장소 미정 — 전 가맹점(ALL) 혜택만 매칭된다
        return PaymentTarget.unspecified();
    }

    private PaymentTarget toPaymentTarget(PaymentTargetRow row) {
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
}
