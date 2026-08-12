package com.wallet.engine.dao;

import com.wallet.engine.dao.dto.BenefitReportRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BenefitReportMapper {

    /**
     * 기준월에 실제로 혜택을 받은 거래를 모두 조회한다.
     *
     * 취소된 거래와 혜택액 0원인 거래는 뺀다 — 리포트는 "받은 혜택"을 보여주는 화면이라
     * 받지 않은 거래가 섞이면 총액이 부풀거나 목록이 의미 없이 길어진다.
     *
     * @param memberId  회원 ID
     * @param yearMonth 기준 연월 (YYYY-MM)
     */
    List<BenefitReportRow> findBenefitedExpenses(@Param("memberId") long memberId,
                                                 @Param("yearMonth") String yearMonth);
}
