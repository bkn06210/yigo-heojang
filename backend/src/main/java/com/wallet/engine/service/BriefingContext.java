package com.wallet.engine.service;

import com.wallet.engine.dao.dto.CategorySpendingRow;

import java.util.List;
import java.util.Map;

/**
 * 브리핑 판정에 필요한, 카드 현황 밖의 값들.
 *
 * 조립기(CardStatusOverviewBuilder)는 DB도 시계도 모른다. 그래야 규칙을 단위 테스트로
 * 고정할 수 있다. 조회는 서비스가 하고 결과만 이 묶음으로 건넨다.
 *
 * @param yearMonth               기준 연월. 문구를 고르는 기준이자 소비 집계의 대상 월
 * @param benefitTargetCategories 혜택 → 대상 업종. 업종을 겨냥하지 않는 혜택(전 가맹점·특정 가맹점)은
 *                                담기지 않는다 — 소비 업종과 맞춰볼 수가 없기 때문이다
 * @param spending                이번 달 업종별 소비. 금액 내림차순
 */
public record BriefingContext(
        String yearMonth,
        Map<Long, Long> benefitTargetCategories,
        List<CategorySpendingRow> spending
) {
}
