package com.wallet.card.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 보유 카드 등록 직후 DB에서 다시 조회한 결과다.
 * 저장된 보유 카드 정보와 연결된 카드 상품 정보를 한 번에 담아 API 응답으로 변환한다.
 */
@Getter
@RequiredArgsConstructor
public class UserCardRegistrationResult {
    private final Long userCardId;
    private final Long cardId;
    private final String cardName;
    private final String companyName;
    private final String cardType;
    private final String maskedCardNumber;
    private final String imageUrl;
    // 추후 대표카드 설정 기능이 추가되면 같은 응답 구조를 재사용할 수 있다.
    private final Boolean representative;
}
