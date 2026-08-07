package com.wallet.card.dto;

import java.time.LocalDateTime;

/**
 * 보유 카드 목록에서 카드 한 장을 표현하는 응답 DTO.
 * <p>
 * user_card 테이블의 보유 카드 정보와
 * card, card_company 테이블의 카드 기본 정보를 조합해서 반환한다.
 * <p>
 * 실적 달성률과 실적 충족 여부는 별도 API에서 조회하므로
 * 이 DTO에는 카드 관리 화면에 필요한 기본 정보만 포함한다.
 */
public record UserCardListItemResponse(
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