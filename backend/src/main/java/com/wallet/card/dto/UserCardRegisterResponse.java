package com.wallet.card.dto;

/**
 * 보유 카드 등록 성공 응답 DTO.
 *
 * 등록 후 프론트가 목록 화면이나 상세 화면으로 이동할 수 있도록
 * 생성된 userCardId와 표시용 카드 정보를 반환한다.
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
