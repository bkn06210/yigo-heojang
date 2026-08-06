package com.wallet.card.dto;

import com.wallet.card.domain.UserCardDetailResult;

import java.time.LocalDateTime;

/**
 * 특정 보유 카드의 상세 정보를 반환하는 응답 DTO다.
 *
 * 현재는 목록 항목과 동일한 정보를 반환하지만,
 * 목록과 상세 API가 서로 독립적으로 변경될 수 있도록
 * 상세 조회 전용 응답 타입으로 분리한다.
 */
public record UserCardDetailResponse(
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
    public static UserCardDetailResponse from(UserCardDetailResult result) {
        return new UserCardDetailResponse(
            result.userCardId(),
            result.cardId(),
            result.cardName(),
            result.issuerName(),
            result.cardType(),
            result.maskedCardNumber(),
            result.imageUrl(),
            result.representative(),
            result.registeredAt()
        );
    }
}