package com.wallet.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wallet.notification.batch.model.PerformanceShortageCandidateRow;

@Mapper
public interface PerformanceShortageCandidateMapper {
    /**
     * 실적 부족 평가 대상 보유카드를 baseYearMonth 기준으로 한 번에 조회한다.
     * 대상 필터는 SQL에서 전부 처리하므로, 반환되는 행은 모두 평가 대상이다.
     *
     * @param baseYearMonth "YYYY-MM" 형식
     */
    List<PerformanceShortageCandidateRow> findCandidateRows(@Param("baseYearMonth") String baseYearMonth);
}