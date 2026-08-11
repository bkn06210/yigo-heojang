package com.wallet.engine.dao;

import com.wallet.engine.dao.dto.BenefitPeriodUsageRow;
import com.wallet.engine.dao.dto.BenefitUsageRow;
import com.wallet.engine.dao.dto.CardMonthlyStateRow;
import com.wallet.engine.dao.dto.CardPerformanceSumRow;
import com.wallet.engine.dao.dto.OptionSelectionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 카드 월 상태 조회 매퍼 — 실적 집계값과 한도 소진 현황.
 *
 * 보유카드 목록(UserCardMapper)과 분리한 이유: 여기는 "이번 달 얼마나 썼나"라는 월 상태이고,
 * 저기는 "무슨 카드를 갖고 있나"라는 보유 정보다. 조회 주기와 갱신 주체가 다르다.
 *
 * <b>회원 단위로 한 번에 조회한다</b> — 보유카드가 여러 장이어도 쿼리 수가 고정된다.
 * 호출자가 userCardId로 그룹핑해 카드별 상태로 나눠 쓴다.
 * 두 조회 모두 user_card를 조인해 회원 소유 + 활성 카드로 제한하므로, 소유권 검증이 조회에 포함된다.
 */
@Mapper
public interface CardStateMapper {

    /**
     * 회원의 활성 보유카드별 월 상태를 <b>기준월과 직전월 두 달치</b> 조회한다.
     *
     * 두 달을 함께 읽는 이유는 전월실적 폴백이다. 기준월 행이 있으면 그 행의
     * prev_performance_amount를 쓰지만, 월이 바뀐 뒤 첫 결제 전이면 기준월 행 자체가 없다.
     * 그때는 직전월 행의 current_performance_amount가 곧 전월실적이므로(이월 전 원본),
     * 한 쿼리로 둘 다 확보해 두면 조회를 늘리지 않고 폴백할 수 있다.
     *
     * <b>직전월 행에서 쓸 수 있는 값은 current_performance_amount뿐이다.</b>
     * shared_limit_used를 직전월 행에서 집으면 이번 달 한도가 이미 소진된 것으로 계산된다.
     * 이 선별은 CardStateAssembler가 baseYearMonth로 판별해 수행한다.
     *
     * @param memberId          회원 ID (소유권 필터)
     * @param baseYearMonth     기준 연월 (YYYY-MM)
     * @param previousYearMonth 직전 연월 (YYYY-MM). 전월실적 폴백용
     */
    List<CardMonthlyStateRow> findStates(@Param("memberId") long memberId,
                                         @Param("baseYearMonth") String baseYearMonth,
                                         @Param("previousYearMonth") String previousYearMonth);

    /**
     * 회원의 활성 보유카드별 혜택 소진 현황을 조회한다.
     *
     * 상태 조회와 달리 <b>기준월 한 달치만</b> 읽는다 — 소진액·횟수는 월마다 리셋되는 값이라
     * 직전월 값을 섞으면 이번 달 한도가 이미 찬 것으로 계산된다.
     *
     * 일 소진 컬럼은 last_applied_date 당일의 값이라 원값 그대로 반환한다 —
     * 오늘이 아닐 때 0으로 접는 리셋 판정은 CardStateAssembler가 한다.
     *
     * @param memberId      회원 ID (소유권 필터)
     * @param baseYearMonth 기준 연월 (YYYY-MM)
     */
    List<BenefitUsageRow> findUsages(@Param("memberId") long memberId,
                                     @Param("baseYearMonth") String baseYearMonth);

    /**
     * 회원의 활성 보유카드별 혜택 소진을 <b>연월 구간으로 합산</b>해 조회한다.
     * 분기 한도면 그 분기의 시작월부터, 연 한도면 1월부터 기준월까지를 넘긴다.
     *
     * 구간 끝을 기준월로 막는 이유는 지난달 현황 조회 때문이다. 분기 전체를 합산하면
     * 조회한 달 이후에 쌓인 소진까지 섞여, 그 달 시점에는 있지도 않던 값으로 한도가 판정된다.
     *
     * 연월이 <code>YYYY-MM</code> 고정 폭 문자열이라 사전순 비교가 곧 시간순 비교다.
     *
     * @param memberId       회원 ID (소유권 필터)
     * @param fromYearMonth  구간 시작 연월 (YYYY-MM, 포함)
     * @param toYearMonth    구간 끝 연월 (YYYY-MM, 포함). 기준월
     */
    List<BenefitPeriodUsageRow> findPeriodUsages(@Param("memberId") long memberId,
                                                 @Param("fromYearMonth") String fromYearMonth,
                                                 @Param("toYearMonth") String toYearMonth);

    /**
     * 회원의 활성 보유카드별 선택형 혜택 선택을 조회한다.
     *
     * 선택 기록이 없는 묶음은 그달에 고르지 않은 것이고, 그 묶음의 혜택은 하나도 적용되지 않는다.
     * 기본값으로 아무거나 켜면 회원이 고르지 않은 혜택을 받은 것으로 기록된다.
     *
     * @param memberId      회원 ID (소유권 필터)
     * @param baseYearMonth 기준 연월 (YYYY-MM)
     */
    List<OptionSelectionRow> findOptionSelections(@Param("memberId") long memberId,
                                                  @Param("baseYearMonth") String baseYearMonth);

    /**
     * 회원의 활성 보유카드별 실적인정액을 연월 구간으로 합산해 조회한다 — 전분기 실적 판정용.
     *
     * 전월실적과 마찬가지로 저장된 집계값(current_performance_amount)을 읽을 뿐 거래를 재합산하지
     * 않는다. 전분기는 그 분기 세 달의 합이므로 직전 분기의 시작월~끝월을 넘긴다.
     *
     * @param memberId      회원 ID (소유권 필터)
     * @param fromYearMonth 구간 시작 연월 (YYYY-MM, 포함)
     * @param toYearMonth   구간 끝 연월 (YYYY-MM, 포함)
     */
    List<CardPerformanceSumRow> findPerformanceSums(@Param("memberId") long memberId,
                                                    @Param("fromYearMonth") String fromYearMonth,
                                                    @Param("toYearMonth") String toYearMonth);
}
