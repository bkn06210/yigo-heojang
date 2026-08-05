package com.wallet.engine.dao;

import com.wallet.engine.dao.dto.BenefitPeriodUsageRow;
import com.wallet.engine.dao.dto.BenefitUsageRow;
import com.wallet.engine.dao.dto.ExpenseRow;
import com.wallet.engine.dao.dto.MonthlyStatusRow;
import com.wallet.engine.dao.dto.OptionSelectionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 정산(결제·취소)의 상태 갱신 매퍼 — 엔진이 처음으로 <b>쓰는</b> 경로다.
 *
 * 추천·현황 조회는 상태 테이블을 읽기만 하지만, 정산은 결제로 소진을 더하고 취소로 되돌린다.
 * 증분/차감은 모두 {@code GREATEST(0, 컬럼 + 델타)} 형태라, 취소가 음수 델타로 와도 0 아래로 내려가지 않는다.
 *
 * @Mapper가 붙어야 MapperScannerConfigurer가 프록시 빈으로 등록한다(root-context.xml의 annotationClass 필터).
 */
@Mapper
public interface SettlementMapper {

    /**
     * 취소할 소비내역 한 건을 역산에 필요한 값과 함께 조회한다.
     *
     * 회원 소유로 제한한다(member_id 조건) — 없거나 남의 것이면 null을 돌려주고,
     * 서비스가 404로 바꾼다(리소스 존재 비노출). 적용 혜택의 통합한도 사용 여부(use_shared_limit)와
     * 카테고리 상위 코드를 조인으로 함께 가져와, 취소 계산에 추가 쿼리가 들지 않게 한다.
     *
     * @param expenseId 소비내역 ID
     * @param memberId  회원 ID (소유권 필터)
     */
    ExpenseRow findExpenseForSettlement(@Param("expenseId") long expenseId,
                                        @Param("memberId") long memberId);

    // ─────────────────────────────────────────────────────────────────────────
    // expense 쓰기 — 취소 API가 자립적·멱등하려면 상태 전환이 이 트랜잭션 안에 있어야 한다.
    //   소비내역 도메인으로 옮기기 쉽도록 쓰기 경로를 이 메서드 하나로 격리해 둔다.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 소비내역을 APPROVED → CANCELED로 전환한다. <b>compare-and-set</b>이다 —
     * 현재 상태가 APPROVED일 때만 바뀌고, 영향받은 행 수를 돌려준다.
     * 0이면 이미 취소된(또는 APPROVED가 아닌) 건이라, 이 한 번의 UPDATE가 중복 취소 차단 latch를 겸한다.
     *
     * @return 실제로 전환된 행 수 (1이면 이번 호출이 취소를 확정, 0이면 이미 취소됨)
     */
    int markExpenseCanceled(@Param("expenseId") long expenseId);

    // ─────────────────────────────────────────────────────────────────────────
    // 엔진 자기 상태 테이블 쓰기
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 카드 월 상태에 당월누적실적·통합한도사용을 가감한다. 결제면 양수, 취소면 음수 델타.
     * 두 값 모두 {@code GREATEST(0, …)}로 하한 0을 보장한다.
     *
     * @return 갱신된 행 수(0이면 해당 (카드, 기준월) 행이 없다는 뜻)
     */
    int applyStateDelta(@Param("userCardId") long userCardId,
                        @Param("baseYearMonth") String baseYearMonth,
                        @Param("performanceDelta") long performanceDelta,
                        @Param("sharedLimitDelta") long sharedLimitDelta);

    /**
     * 혜택별 월 소진(누적 혜택액·적용횟수)을 가감한다. 결제면 양수, 취소면 음수 델타.
     * 일 소진(daily_*)·최종적용일시는 여기서 건드리지 않는다 — 취소 시 되돌리지 않는 것이 명세다
     * (이력이 없어 일1회 판정만 보수적으로 유지).
     *
     * @return 갱신된 행 수(0이면 해당 (카드, 혜택, 기준월) 소진 행이 없다는 뜻)
     */
    int applyUsageDelta(@Param("userCardId") long userCardId,
                        @Param("benefitId") long benefitId,
                        @Param("baseYearMonth") String baseYearMonth,
                        @Param("amountDelta") long amountDelta,
                        @Param("countDelta") int countDelta);

    // ─────────────────────────────────────────────────────────────────────────
    // 결제 가산 (없으면 INSERT, 있으면 누적 — 월 롤오버·일 소진 리셋 포함)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 카드 월 상태에 실적·통합한도 소진을 더한다. 기준월 행이 없으면 만들면서
     * prev_performance_amount를 이월값으로 채운다(월 롤오버). 있으면 두 소진값만 누적한다
     * (prev는 월 중에 바뀌지 않으므로 갱신하지 않는다).
     *
     * @param prevPerformanceAmount 새 행을 만들 때 채울 전월실적(직전월 누적액). 기존 행이면 무시된다
     * @param performanceDelta      더할 당월누적실적 인정액
     * @param sharedLimitDelta      더할 통합한도 소진액
     */
    void upsertMonthlyStateAdd(@Param("userCardId") long userCardId,
                               @Param("baseYearMonth") String baseYearMonth,
                               @Param("prevPerformanceAmount") long prevPerformanceAmount,
                               @Param("performanceDelta") long performanceDelta,
                               @Param("sharedLimitDelta") long sharedLimitDelta);

    /**
     * 혜택별 월 소진을 더한다. 행이 없으면 만든다. 일 소진(daily_*)은 last_applied_date가
     * 결제일과 같으면 누적, 다르면 그 날짜 기준으로 리셋한다.
     *
     * @param discount    이번 결제로 받은 혜택액
     * @param appliedDate 결제 일자 (일 소진 리셋 판정·last_applied_date 갱신)
     */
    void upsertBenefitUsageAdd(@Param("userCardId") long userCardId,
                               @Param("benefitId") long benefitId,
                               @Param("baseYearMonth") String baseYearMonth,
                               @Param("discount") long discount,
                               @Param("appliedDate") java.time.LocalDate appliedDate);

    // ─────────────────────────────────────────────────────────────────────────
    // 갱신 후 현황 조회 (취소 응답 = 차감 반영된 CardMonthlyStatus)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 카드 한 장의 월 상태를 카드명과 함께 조회한다. 목표금액·달성률 같은 파생값은 담지 않는다
     * (조회 시 계산). 없으면 null — 해당 (카드, 기준월) 상태 행이 없다는 뜻이다.
     *
     * @param userCardId    보유카드 ID
     * @param baseYearMonth 기준 연월 (YYYY-MM)
     */
    MonthlyStatusRow findMonthlyStatus(@Param("userCardId") long userCardId,
                                       @Param("baseYearMonth") String baseYearMonth);

    /**
     * 카드 한 장의 기준월 혜택별 소진 행을 조회한다 — 현황 응답의 benefits[] 누적 혜택액용.
     *
     * 회원 단위로 읽는 추천 경로(CardStateMapper.findUsages)와 달리 카드 하나로 좁힌다.
     * 여기서는 used_amount(월 누적)만 쓰므로 일 소진 리셋 판정은 필요 없다.
     *
     * @param userCardId    보유카드 ID
     * @param baseYearMonth 기준 연월 (YYYY-MM)
     */
    List<BenefitUsageRow> findUsagesByUserCard(@Param("userCardId") long userCardId,
                                               @Param("baseYearMonth") String baseYearMonth);

    /**
     * 카드 한 장의 혜택 소진을 연월 구간으로 합산해 조회한다 — 분기·연 한도 판정용.
     * 회원 단위로 읽는 조회 경로(CardStateMapper.findPeriodUsages)와 달리 카드 하나로 좁힌다.
     *
     * @param userCardId    보유카드 ID
     * @param fromYearMonth 구간 시작 연월 (YYYY-MM, 포함)
     * @param toYearMonth   구간 끝 연월 (YYYY-MM, 포함). 기준월
     */
    List<BenefitPeriodUsageRow> findPeriodUsagesByUserCard(@Param("userCardId") long userCardId,
                                                           @Param("fromYearMonth") String fromYearMonth,
                                                           @Param("toYearMonth") String toYearMonth);

    /**
     * 카드 한 장의 선택형 혜택 선택을 조회한다. 기록이 없는 묶음은 그달에 고르지 않은 것이다.
     *
     * @param userCardId    보유카드 ID
     * @param baseYearMonth 기준 연월 (YYYY-MM)
     */
    List<OptionSelectionRow> findOptionSelectionsByUserCard(@Param("userCardId") long userCardId,
                                                            @Param("baseYearMonth") String baseYearMonth);

    /**
     * 카드 한 장의 실적인정액을 연월 구간으로 합산해 조회한다 — 전분기 실적 판정용.
     * 그 기간에 상태 행이 하나도 없으면 0이다(실적이 실제로 0이므로 정답이다).
     *
     * @param userCardId    보유카드 ID
     * @param fromYearMonth 구간 시작 연월 (YYYY-MM, 포함)
     * @param toYearMonth   구간 끝 연월 (YYYY-MM, 포함)
     */
    long findPerformanceSumByUserCard(@Param("userCardId") long userCardId,
                                      @Param("fromYearMonth") String fromYearMonth,
                                      @Param("toYearMonth") String toYearMonth);
}
