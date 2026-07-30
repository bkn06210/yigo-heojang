package com.wallet.engine.dao;

import com.wallet.engine.dao.dto.BenefitUsageRow;
import com.wallet.engine.dao.dto.CardMonthlyStateRow;
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
}
