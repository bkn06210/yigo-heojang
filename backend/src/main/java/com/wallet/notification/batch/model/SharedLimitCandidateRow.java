package com.wallet.notification.batch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 카드 통합할인한도 후보 계산을 위한 조회 결과 (규칙 문서 3.5절)
 * sharedMonthlyLimit이 0보다 큰 카드만 SQL에서 걸러서 담아오므로, 이 행이 존재한다는 것
 * 자체가 "이 카드는 통합한도 평가 대상이다"라는 뜻이다.
 */
@Getter
@Builder
@AllArgsConstructor
public class SharedLimitCandidateRow {
    private long userCardId;
    private long memberId;
    private long cardId;
    private String cardName;
    private long sharedMonthlyLimit;
    private long sharedLimitUsed;
}