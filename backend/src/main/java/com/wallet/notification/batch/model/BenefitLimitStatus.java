package com.wallet.notification.batch.model;

/** 혜택 월 한도 소진 상태 (규칙 문서 3.6절) */
public enum BenefitLimitStatus {
    /** 80% 이상 100% 미만 */
    NEAR,
    /** 100% 이상 */
    EXHAUSTED
}