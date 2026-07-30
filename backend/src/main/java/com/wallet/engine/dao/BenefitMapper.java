package com.wallet.engine.dao;

import com.wallet.engine.dao.dto.BenefitExclusionRow;
import com.wallet.engine.dao.dto.BenefitRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 혜택 규칙 조회 매퍼 — 추천 계산에 쓸 카드별 활성 혜택을 가져온다.
 *
 * @Mapper가 붙어야 MapperScannerConfigurer가 프록시 빈으로 등록한다(root-context.xml의 annotationClass 필터).
 * 반환은 전부 flat row DTO다 — BenefitCandidate로의 조립은 BenefitCandidateAssembler가 한다.
 */
@Mapper
public interface BenefitMapper {

    /**
     * 카드의 활성 혜택(is_active='Y')을 판정된 실적구간의 개별한도와 함께 조회한다.
     *
     * benefit_tier_limit을 tierId로 LEFT JOIN해 구간별 월한도·혜택값을 함께 가져온다.
     * 해당 구간에 개별한도 행이 없으면 NULL이며, 조립 단계의 TierLimitResolver가 base 값을 유지한다.
     * <b>매칭 여부와 무관하게 카드의 활성 혜택 전부를 반환한다</b> — 묶음(limit_group_code) 한도
     * 합산이 이번 결제와 무관한 혜택의 소진분까지 봐야 하기 때문이다(CardBenefitSelector 계약).
     *
     * @param cardId 카드 마스터 ID
     * @param tierId 판정된 실적구간 ID (benefit_tier_limit 조회 키)
     */
    List<BenefitRow> findActiveBenefits(@Param("cardId") long cardId, @Param("tierId") long tierId);

    /** 카드의 활성 혜택에 걸린 제외 규칙을 조회한다. benefitId로 그룹핑해 각 혜택에 붙인다. */
    List<BenefitExclusionRow> findExclusions(@Param("cardId") long cardId);
}
