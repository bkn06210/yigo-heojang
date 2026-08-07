package com.wallet.card.domain;

import java.time.LocalDateTime;

// 보유 카드 상세 조회 쿼리 결과를 담는 객체
public record UserCardDetailResult(
    Long userCardId,
    Long cardId,
    String cardName,
    String issuerName,
    String cardType,
    String maskedCardNumber,
    String imageUrl,
    Boolean representative,
    LocalDateTime registeredAt
) {
}