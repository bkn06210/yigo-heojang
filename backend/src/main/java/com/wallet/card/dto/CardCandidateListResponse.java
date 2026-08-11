package com.wallet.card.dto;

import java.util.List;

/**
 * 카드 상품 후보 목록 응답 DTO.
 * <p>
 * BIN으로 식별된 카드사 정보와
 * 사용자가 선택할 수 있는 카드 상품 목록을 함께 반환한다.
 */
public record CardCandidateListResponse(
    String issuerName,
    String lastFourDigits,
    List<CardCandidateResponse> cards
) {
}