package com.wallet.card.dto;

import javax.validation.constraints.NotBlank;

/**
 * 보유 카드 등록 요청 DTO.
 * <p>
 * 클라이언트는 카드 상품을 선택하지 않고 카드번호만 전달한다.
 * 서버가 전체 번호와 Mock 데이터를 비교해서 연결할 카드 상품을 결정한다.
 */
public record UserCardRegisterRequest(
    @NotBlank(message = "카드번호를 입력해 주세요.")
    String cardNumber
) {
}
