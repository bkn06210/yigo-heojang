package com.wallet.card.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * 보유 카드 최종 등록 요청 DTO.
 * <p>
 * 후보 조회 단계에서 사용자가 카드 상품을 고른 뒤,
 * 최종 등록 API로 전달하는 요청 값이다.
 */
public record UserCardRegisterRequest(
    @NotNull(message = "카드 상품을 선택해 주세요.")
    @Positive(message = "카드 상품 ID가 올바르지 않습니다.")
    Long cardId,

    @NotBlank(message = "카드번호를 입력해 주세요.")
    String cardNumber
) {
}
