package com.wallet.engine.service;

import com.wallet.engine.assembler.PerformanceInputAssembler;
import com.wallet.engine.calculator.PerformanceAmountCalculator;
import com.wallet.engine.calculator.PerformanceTierResolver;
import com.wallet.engine.dao.PerformanceMapper;
import com.wallet.engine.model.IgnoredExclusion;
import com.wallet.engine.model.PerformanceAmountResult;
import com.wallet.engine.model.PerformanceExclusion;
import com.wallet.engine.model.PerformanceStatus;
import com.wallet.engine.model.PerformanceTier;
import com.wallet.engine.model.PerformanceTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 전월실적 판정 서비스 — 순수 계산기 두 개를 DAO 조회로 연결한다.
 *
 * 계산기(PerformanceAmountCalculator, PerformanceTierResolver)는 Spring·DB·시계를 모른다.
 * 이 서비스가 "어느 달인가"를 정해 기간 경계를 만들고, 조회 결과를 assembler로 도메인 모델로
 * 바꿔 계산기에 먹인 뒤, 판정 결과(PerformanceStatus)를 돌려준다.
 *
 * 계산기는 스테이트리스 순수 함수라 스프링 빈으로 두지 않고 여기서 직접 인스턴스화한다 —
 * 계산기를 프레임워크에서 떼어 두려는 설계(순수성)를 유지하기 위함이다.
 */
@Service
public class PerformanceEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(PerformanceEvaluationService.class);

    private final PerformanceMapper performanceMapper;
    private final PerformanceInputAssembler assembler;

    private final PerformanceAmountCalculator amountCalculator = new PerformanceAmountCalculator();
    private final PerformanceTierResolver tierResolver = new PerformanceTierResolver();

    public PerformanceEvaluationService(PerformanceMapper performanceMapper,
                                        PerformanceInputAssembler assembler) {
        this.performanceMapper = performanceMapper;
        this.assembler = assembler;
    }

    /**
     * 특정 보유카드의 전월실적을 계산해 적용 실적구간을 판정한다.
     *
     * @param userCardId 보유카드 ID (거래·상태 조회 키)
     * @param cardId     카드 마스터 ID (실적구간·제외 규칙 조회 키)
     * @param baseMonth  기준 연월. 전월실적은 이 달의 '직전 달' 거래로 계산한다
     * @return 판정된 구간과 통합한도(PerformanceStatus)
     */
    @Transactional(readOnly = true)
    public PerformanceStatus evaluate(long userCardId, long cardId, YearMonth baseMonth) {
        long prevPerformanceAmount = calculatePreviousMonthPerformance(userCardId, cardId, baseMonth);

        List<PerformanceTier> tiers = assembler.toTiers(performanceMapper.findTiers(cardId));
        return tierResolver.judge(tiers, prevPerformanceAmount);
    }

    /**
     * 전월실적인정액을 계산한다. 직전 달 [1일 00:00, 당월 1일 00:00) 거래를 조회해
     * 실적 제외 규칙을 적용한 합계다.
     */
    private long calculatePreviousMonthPerformance(long userCardId, long cardId, YearMonth baseMonth) {
        YearMonth previousMonth = baseMonth.minusMonths(1);
        LocalDateTime fromInclusive = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime toExclusive = baseMonth.atDay(1).atStartOfDay();

        List<PerformanceTransaction> transactions = assembler.toTransactions(
                performanceMapper.findTransactionsInPeriod(userCardId, fromInclusive, toExclusive));
        List<PerformanceExclusion> exclusions = assembler.toExclusions(
                performanceMapper.findExclusions(cardId));

        PerformanceAmountResult result = amountCalculator.calculate(transactions, exclusions);
        logIgnoredExclusions(cardId, result.ignored());
        return result.amount();
    }

    /**
     * 해석하지 못해 무시한 제외 규칙을 운영자 로그로 남긴다 — 시드/데이터 미비 신호다.
     * 챗봇용 사용자 답변("이 규칙이 반영되지 않았다") 전달은 이후 단계에서 붙인다.
     */
    private void logIgnoredExclusions(long cardId, List<IgnoredExclusion> ignored) {
        if (ignored.isEmpty()) {
            return;
        }
        for (IgnoredExclusion item : ignored) {
            log.warn("실적 제외 규칙 무시 (cardId={}): type={}, value={}, 사유={}",
                    cardId, item.rawType(), item.rawValue(), item.reason());
        }
    }
}
