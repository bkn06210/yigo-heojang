package com.wallet.card.dto;

/**
 * 보유 카드 등록 성공 응답 DTO.
 *
 * 클라이언트가 카드 상품을 고르지 않으므로, 서버가 어떤 카드로 매칭했는지
 * 이 응답으로 알려준다. 프론트는 이 값으로 등록 완료 화면을 그린다.
 */
public record UserCardRegisterResponse(
    Long userCardId,
    Long cardId,
    String cardName,
    String issuerName,
    String cardType,
    String maskedCardNumber,
    String imageUrl,
    Boolean representative
) {
}
