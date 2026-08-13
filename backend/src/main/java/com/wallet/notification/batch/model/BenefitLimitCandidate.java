package com.wallet.notification.batch.model;

/**
 * 혜택 월 한도 임박·소진 알림 후보 (규칙 문서 3장)
 * <p>
 * unit에 따라 benefitId/benefitName/limitGroupCode 중 어느 필드가 채워지는지가 다르다.
 * INDIVIDUAL → benefitId, benefitName만 값이 있고 limitGroupCode는 null
 * GROUP      → limitGroupCode만 값이 있고 benefitId, benefitName은 null
 * SHARED     → 셋 다 null (카드 단위라 혜택을 특정하지 않는다)
 *
 * @param deduplicationKey 3.8절 형식(개별/그룹/카드 통합마다 접두사 B/G/S가 다르다)
 */
public record BenefitLimitCandidate(
    Long memberId,
    Long userCardId,
    Long cardId,
    String cardName,
    BenefitLimitUnit unit,
    Long benefitId,
    String benefitName,
    String limitGroupCode,
    long limitAmount,
    long usedAmount,
    double usageRate,
    BenefitLimitStatus status,
    String yearMonth,
    String deduplicationKey
) {
}