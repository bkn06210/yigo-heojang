package com.wallet.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wallet.notification.batch.model.BenefitCandidateRow;
import com.wallet.notification.batch.model.SharedLimitCandidateRow;

@Mapper
public interface BenefitLimitCandidateMapper {

    /**
     * 개별·그룹 혜택 한도 평가 대상을 baseYearMonth 기준으로 한 번에 조회한다.
     * limitGroupCode가 NULL이면 개별 후보, 값이 있으면 계산기가 같은 카드·같은 코드끼리
     * 묶어서 그룹 후보를 만든다.
     *
     * @param baseYearMonth 기준 연월 "YYYY-MM"
     * @param prevYearMonth baseYearMonth의 전월 "YYYY-MM" — 전월실적 보정 조회(3.2절)에 사용
     */
    List<BenefitCandidateRow> findBenefitCandidateRows(
        @Param("baseYearMonth") String baseYearMonth,
        @Param("prevYearMonth") String prevYearMonth
    );

    /** 카드 통합할인한도 평가 대상을 조회한다 (규칙 문서 3.5절) */
    List<SharedLimitCandidateRow> findSharedLimitCandidateRows(
        @Param("baseYearMonth") String baseYearMonth,
        @Param("prevYearMonth") String prevYearMonth
    );
}