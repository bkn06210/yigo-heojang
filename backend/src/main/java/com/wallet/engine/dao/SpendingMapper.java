package com.wallet.engine.dao;

import com.wallet.engine.dao.dto.CategorySpendingRow;
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
}
