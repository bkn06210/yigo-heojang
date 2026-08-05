package com.wallet.card.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 카드 상품 후보 조회 요청 DTO.
 * <p>
 * 사용자가 카드번호, 유효기간, CVC를 입력하면
 * 서버는 입력값을 검증한 뒤 BIN으로 카드사를 식별하고,
 * 해당 카드사의 카드 상품 후보를 내려준다.
 */
public record CardIdentificationRequest(
    @NotBlank(message = "카드번호를 입력해 주세요.")
    String cardNumber
) {

}