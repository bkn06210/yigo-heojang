package com.wallet.notification.batch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 혜택 월 한도 후보 계산을 위한 조회 결과 (규칙 문서 3장)
 * <p>
 * 개별·그룹 후보 모두 이 한 가지 행 모양에서 출발한다 — 개별은 이 행 하나가 후보 하나가 되고,
 * 그룹은 limitGroupCode가 같은 행 여러 개를 계산기가 묶어서 후보 하나로 합친다.
 * <p>
 * effectiveMonthlyLimit은 "이 카드에 지금 적용 중인 실적구간"(전월실적으로 판정, 3.2절)의
 * benefit_tier_limit.tier_monthly_limit이 있으면 그 값, 없으면 benefit.monthly_limit을
 * SQL에서 이미 골라서 담아온다. NULL이면 "이 혜택은 월 한도가 없다"는 뜻이다.
 * <p>
 * monthPerformanceMet은 그 실적구간의 min_performance_amount가 0보다 큰지를 담는다 —
 * require_performance='Y' AND performance_period='MONTH'인 혜택이 "이번 달 조건을
 * 충족했는지"를 판정하는 데 쓴다(3.3절).
 * <p>
 * require_performance·performance_period·limit_group_code는 SQL에서 필터링하지 않고
 * 그대로 담아 보낸다. "그룹 구성원 중 QUARTER가 있으면 그룹 전체 제외"(3.4절)처럼 다른 행을
 * 참고해야 하는 판단은 SQL의 WHERE로 한 행씩 잘라내는 순간 불가능해지기 때문에, 그 판단은
 * 전부 계산기(Java)로 넘긴다.
 */
@Getter
@Builder
@AllArgsConstructor
public class BenefitCandidateRow {
    private long userCardId;
    private long memberId;
    private long cardId;
    private String cardName;
    private long benefitId;
    private String benefitName;
    /** NULL이면 그룹에 속하지 않은 개별 혜택 */
    private String limitGroupCode;
    /** 'Y' | 'N' */
    private String requirePerformance;
    /** 'MONTH' | 'QUARTER' */
    private String performancePeriod;
    private Long effectiveMonthlyLimit;
    private boolean monthPerformanceMet;
    private long usedAmount;
}