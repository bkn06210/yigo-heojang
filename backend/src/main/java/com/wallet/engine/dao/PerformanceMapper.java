package com.wallet.engine.dao;

import com.wallet.engine.dao.dto.PerformanceExclusionRow;
import com.wallet.engine.dao.dto.PerformanceTierRow;
import com.wallet.engine.dao.dto.PerformanceTransactionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 전월실적 판정에 필요한 조회 매퍼.
 *
 * @Mapper가 붙어야 MapperScannerConfigurer가 프록시 빈으로 등록한다(root-context.xml의 annotationClass 필터).
 * 반환은 전부 flat row DTO다 — 불변 계산기 모델로의 조립은 PerformanceInputAssembler가 한다.
 */
@Mapper
public interface PerformanceMapper {

    /**
     * 특정 보유카드의 [from, to) 기간 거래를 실적 판정에 필요한 컬럼만 조회한다.
     *
     * "어느 달인가"는 호출자가 경계로 정한다 — 계산기가 시계를 모르는 것과 대칭이다.
     * 취소 건은 여기서 제외하지만(payment_status &lt;&gt; 'CANCELED'), 계산기도 다시 거른다(이중 방어).
     *
     * @param userCardId    보유카드 ID
     * @param fromInclusive 조회 시작(포함) — 전월 1일 00:00
     * @param toExclusive   조회 끝(미포함) — 당월 1일 00:00
     */
    List<PerformanceTransactionRow> findTransactionsInPeriod(
            @Param("userCardId") long userCardId,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive);

    /** 카드의 전월실적 제외 규칙을 조회한다. */
    List<PerformanceExclusionRow> findExclusions(@Param("cardId") long cardId);

    /** 카드 여러 장의 실적 제외를 한 번에. 카드 수만큼 쿼리가 늘지 않게 한다 */
    List<PerformanceExclusionRow> findExclusionsByCardIds(@Param("cardIds") List<Long> cardIds);

    /** 카드의 실적구간을 조회한다. 0원 구간을 포함해 최소 1행이 있어야 판정이 성립한다. */
    List<PerformanceTierRow> findTiers(@Param("cardId") long cardId);

    /**
     * 여러 카드의 실적구간을 한 번에 조회한다 — 보유카드 전부를 판정하는 추천 흐름용.
     *
     * 호출자가 cardId로 그룹핑해 카드별 구간 목록으로 나눠 쓴다.
     * cardIds가 비어 있으면 호출하지 않는다(빈 IN 절은 SQL 문법 오류다).
     */
    List<PerformanceTierRow> findTiersByCardIds(@Param("cardIds") List<Long> cardIds);
}
