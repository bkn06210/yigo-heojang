package com.wallet.engine.dao;

import com.wallet.engine.dao.dto.CategorySpendingRow;
import com.wallet.engine.dao.dto.SpentTransactionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SpendingMapper {

    /**
     * 기준월의 업종별 소비 집계.
     *
     * 혜택 리포트(BenefitReportMapper)와 조회 대상이 다르다. 저쪽은 혜택을 받은 거래만 보는데
     * 이쪽은 결제 전부를 본다 — 브리핑이 찾는 것은 "결제는 했는데 혜택은 못 받은" 자리라,
     * 받은 것만 보면 그 자리가 보이지 않는다.
     *
     * @param memberId  회원 ID
     * @param yearMonth 기준 연월 (YYYY-MM)
     */
    List<CategorySpendingRow> findMonthlySpendingByCategory(@Param("memberId") long memberId,
                                                            @Param("yearMonth") String yearMonth);

    /**
     * 그달의 결제를 거래 단위로 조회한다. 카드 추천 시뮬레이션의 입력이다.
     *
     * 집계가 아니라 거래 하나하나를 내리는 이유는 한도가 결제 건수에 따라 걸리기 때문이다.
     * 같은 20만원이라도 5천원 40번과 5만원 4번은 받는 혜택이 다르다.
     *
     * 취소 건은 뺀다 — 하지 않은 소비를 근거로 카드를 권하게 된다.
     */
    List<SpentTransactionRow> findMonthlyTransactions(@Param("memberId") long memberId,
                                                      @Param("yearMonth") String yearMonth);
}
