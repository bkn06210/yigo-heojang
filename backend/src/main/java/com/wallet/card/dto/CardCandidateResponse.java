package com.wallet.card.dto;

/**
 * 등록 후보 카드 상품 응답 DTO.
 * <p>
 * 사용자가 카드 상품을 잘못 선택하지 않도록
 * 카드명뿐 아니라 카드사명, 카드 이미지, 카드 유형을 함께 내려준다.
 */
public record CardCandidateResponse(
    Long cardId,
    String cardName,
    String issuerName,
    String cardType,
    String imageUrl,
    String description
) {
}