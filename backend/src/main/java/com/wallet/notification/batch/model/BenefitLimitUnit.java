package com.wallet.notification.batch.model;

/** 혜택 월 한도의 평가 단위 (규칙 문서 3.1절) */
public enum BenefitLimitUnit {
    /** userCardId + benefitId */
    INDIVIDUAL,
    /** userCardId + limitGroupCode */
    GROUP,
    /** userCardId (카드 통합할인한도) */
    SHARED
}