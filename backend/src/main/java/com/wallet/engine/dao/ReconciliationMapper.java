package com.wallet.engine.dao;

import com.wallet.engine.dao.dto.CardMonthlyStateRow;
import com.wallet.engine.dao.dto.ReconciliationBenefitSumRow;
import com.wallet.engine.dao.dto.ReconciliationTransactionRow;
import com.wallet.engine.dao.dto.ReconciliationUsageRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상태 대사용 조회 매퍼 — 저장된 상태와, 그 상태가 나왔어야 할 원본(소비내역)을 기간 단위로 읽는다.
 *
 * <b>전부 읽기 전용이고, 조회 수가 회원·카드 수와 무관하다.</b> 카드마다 조회하지 않고 기간 전체를
 * 한 번에 읽어 호출자가 (보유카드, 월)로 나눠 쓴다.
 *
 * 다른 조회 매퍼와 달리 카드 상태(ACTIVE)로 거르지 않는다 — 해지한 카드의 상태도 어긋날 수 있고,
 * 거르면 그 어긋남이 영영 보이지 않는다.
 *
 * memberId는 선택이다. null이면 전 회원, 값이 있으면 그 회원이 보유한 카드로 좁힌다.
 * 상태 테이블에는 member_id가 없어 양쪽 모두 user_card 소유자로 좁힌다 — 한쪽만 다른 기준으로
 * 좁히면 범위가 어긋나 없는 불일치가 만들어진다.
 */
@Mapper
public interface ReconciliationMapper {

    /**
     * 저장된 카드 월 상태를 연월 구간으로 조회한다.
     *
     * 전월실적 이월을 맞춰 보려면 대조 시작 월의 직전 월 행이 필요하므로, 호출자가 구간을 한 달 앞에서 시작한다.
     *
     * @param fromYearMonth 시작 연월 (YYYY-MM, 포함)
     * @param toYearMonth   끝 연월 (YYYY-MM, 포함)
     */
    List<CardMonthlyStateRow> findStoredStates(@Param("fromYearMonth") String fromYearMonth,
                                               @Param("toYearMonth") String toYearMonth,
                                               @Param("memberId") Long memberId);

    /** 저장된 혜택 소진을 연월 구간으로 조회한다. 혜택의 계산방식을 함께 읽는다. */
    List<ReconciliationUsageRow> findStoredUsages(@Param("fromYearMonth") String fromYearMonth,
                                                  @Param("toYearMonth") String toYearMonth,
                                                  @Param("memberId") Long memberId);

    /**
     * 혜택이 적용된 소비내역을 (보유카드, 혜택, 월)로 묶어 할인액 합과 건수를 구한다.
     *
     * 합계는 규칙 없이 더하기만 하면 되는 값이라 DB가 집계한다 — 행을 가져와 자바에서 더할 이유가 없다.
     *
     * @param fromInclusive 조회 시작(포함) — 시작 월 1일 00:00
     * @param toExclusive   조회 끝(미포함) — 끝 월의 다음 달 1일 00:00
     */
    List<ReconciliationBenefitSumRow> findAppliedBenefitSums(@Param("fromInclusive") LocalDateTime fromInclusive,
                                                             @Param("toExclusive") LocalDateTime toExclusive,
                                                             @Param("memberId") Long memberId);

    /**
     * 기간 안의 소비내역을 실적 판정에 필요한 컬럼으로 조회한다. 취소 건도 포함한다(계산기가 거른다).
     *
     * 할인액 합과 달리 집계하지 않고 행으로 가져온다. 실적 인정 여부는 거래마다 제외 규칙으로
     * 판정해야 하는데, 그 규칙을 SQL로 다시 쓰면 계산기와 SQL 두 곳에 같은 규칙이 생겨
     * 한쪽만 바뀌는 순간 대사가 없는 불일치를 보고한다.
     */
    List<ReconciliationTransactionRow> findTransactions(@Param("fromInclusive") LocalDateTime fromInclusive,
                                                        @Param("toExclusive") LocalDateTime toExclusive,
                                                        @Param("memberId") Long memberId);
}
