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

    /** 카드의 실적구간을 조회한다. 0원 구간을 포함해 최소 1행이 있어야 판정이 성립한다. */
    List<PerformanceTierRow> findTiers(@Param("cardId") long cardId);

    /**
     * 여러 카드의 실적구간을 한 번에 조회한다 — 보유카드 전부를 판정하는 추천 흐름용.
     *
     * 호출자가 cardId로 그룹핑해 카드별 구간 목록으로 나눠 쓴다.
     * cardIds가 비어 있으면 호출하지 않는다(빈 IN 절은 SQL 문법 오류다).
     */
    List<PerformanceTierRow> findTiersByCardIds(@Param("cardIds") List<Long> cardIds);

    /**
     * 회원의 평균 결제액을 조회한다 — 금액을 넣지 않고 추천을 받을 때 쓴다.
     *
     * 엔진은 금액 없이는 계산할 수 없다(할인 한도·건당 최소금액·적립률이 전부 금액에 걸린다).
     * 그렇다고 아무 상수나 넣으면 그 사람 소비와 무관한 추천이 나오므로, 본인 결제 이력의 평균을 쓴다.
     *
     * categoryId 를 주면 그 업종의 평균만 본다 — 카페를 고른 사람에게 주유 평균을 적용하면 어긋난다.
     * 그 업종 이력이 없으면 호출자가 업종 없이 다시 물어본다.
     *
     * 취소 건은 뺀다. 실제로 쓴 금액이 아니라서 평균을 왜곡한다.
     * 이력이 없으면 null 을 돌려준다(0이 아니다 — 호출자가 "없음"과 구분해야 한다).
     *
     * @param memberId      회원 ID
     * @param categoryId    업종 ID. null 이면 업종을 가리지 않는다
     * @param fromInclusive 조회 시작(포함). 오래된 소비까지 섞으면 지금 씀씀이와 멀어진다
     */
    Long findAverageExpenseAmount(@Param("memberId") long memberId,
                                  @Param("categoryId") Long categoryId,
                                  @Param("fromInclusive") LocalDateTime fromInclusive);
}
