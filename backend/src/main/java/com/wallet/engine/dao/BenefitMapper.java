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
     * 구간별 개별한도는 혜택의 실적 기간 축에 맞는 구간에서 가져온다. 한 카드가 전월 축과
     * 전분기 축 구간표를 함께 가질 수 있어, 하나의 tierId로 조인하면 분기 혜택이 월 구간의
     * 개별한도를 쓰게 된다(에러 없이 한도만 틀린다).
     *
     * @param cardId        카드 ID
     * @param monthTierId   전월 실적으로 판정된 구간 ID
     * @param quarterTierId 전분기 실적으로 판정된 구간 ID. 분기 구간표가 없는 카드면 null
     */
    List<BenefitRow> findActiveBenefits(@Param("cardId") long cardId,
                                        @Param("monthTierId") long monthTierId,
                                        @Param("quarterTierId") Long quarterTierId);

    /** 카드의 활성 혜택에 걸린 제외 규칙을 조회한다. benefitId로 그룹핑해 각 혜택에 붙인다. */
    List<BenefitExclusionRow> findExclusions(@Param("cardId") long cardId);

    /**
     * 소비한 업종·가맹점에 혜택이 있는 <b>미보유</b> 카드를 추린다 — 추천 후보 축소.
     *
     * 카드 전체를 시뮬레이션하면 카드 수만큼 계산이 늘어난다. 어차피 그 회원이 쓰지 않는 업종의
     * 카드는 순증이 0이므로 미리 걸러낸다. 맞는 혜택이 많은 카드부터 준다.
     *
     * 계산 대상이 아닌 혜택 종류(GIFT·무이자할부·사후정산)는 후보 판정에서도 뺀다 —
     * 그 혜택만 가진 카드가 후보로 올라오면 시뮬레이션 결과가 늘 0이다.
     */
    List<Long> findCandidateCardIds(@Param("categoryIds") List<Long> categoryIds,
                                    @Param("merchantIds") List<Long> merchantIds,
                                    @Param("excludeCardIds") List<Long> excludeCardIds,
                                    @Param("limit") int limit);
}
