package com.wallet.engine.dto;

/**
 * 홈 위젯 "남은 혜택" 한 줄 — 전체 보유 카드 현황(#2)의 benefitsSummary[] 항목.
 *
 * 개별 카드 상세(#3)의 {@link BenefitUsageStatus}와 달리 <b>묶음 한도 그룹은 한 줄로 접어</b> 내려간다.
 * 목록 화면은 잔여액을 세로로 나열하므로, 한도를 공유하는 혜택을 따로 내려주면 화면이 그대로 더해
 * 실제 지갑보다 몇 배 큰 금액으로 보인다(5,000원 한도 하나가 3줄 × 5,000원 = 15,000원처럼).
 *
 * @param benefitId      대표 혜택 ID (묶음이면 그룹에서 가장 작은 benefit_id)
 * @param benefitName    표시명. 묶음이면 "대표 혜택명 외 N건"
 * @param limitGroupCode 묶음 한도 코드. null이면 이 혜택 단독
 * @param remainingLimit 잔여 한도. 묶음이면 그룹 기준 잔여액, <b>한도 없는 혜택이면 null</b>(제약 없음)
 */
public record BenefitSummary(
        long benefitId,
        String benefitName,
        String limitGroupCode,
        Long remainingLimit
) {
}
