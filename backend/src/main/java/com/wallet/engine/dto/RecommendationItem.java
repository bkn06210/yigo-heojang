package com.wallet.engine.dto;

import com.wallet.engine.model.BenefitKind;

/**
 * 카드 한 장의 추천 결과 — 응답 recommendations[]의 원소.
 *
 * expectedBenefit는 <b>카드당 혜택 1개만 적용한 값</b>이다(합산 아님). 여러 혜택이 매칭돼도
 * 혜택액이 가장 큰 하나만 쓴다 — 실제 약관의 "타 할인과 중복 불가"가 기본이기 때문이다.
 *
 * 혜택이 하나도 적용되지 않은 카드도 목록에 담는다(표시 개수는 화면이 자른다).
 * 그 경우 benefitId·benefitKind는 null, expectedBenefit는 0이다.
 *
 * @param rank            추천 순위(1이 최적)
 * @param userCardId      보유카드 id
 * @param cardName        카드명
 * @param expectedBenefit 예상 혜택액(원, 원 미만 절사)
 * @param isEstimate      true=예상(정률+구간 대표금액), false=확정(정액이거나 상한 도달)
 * @param benefitKind     적용된 혜택 종류. 적용된 혜택이 없으면 null
 * @param reason          추천 근거 — 혜택명과 계산 금액으로 엔진이 조립한다
 * @param dynamicSwitch   동적 전환 여부. 한도·횟수를 무시했을 때의 1위와 실제 1위가 다를 때 true
 */
public record RecommendationItem(
        int rank,
        long userCardId,
        String cardName,
        long expectedBenefit,
        boolean isEstimate,
        BenefitKind benefitKind,
        String reason,
        boolean dynamicSwitch
) {
}
