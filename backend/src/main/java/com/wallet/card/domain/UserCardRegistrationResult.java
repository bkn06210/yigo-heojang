package com.wallet.card.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserCardRegistrationResult {
    private final Long userCardId;
    private final String cardName;
    private final String companyName;
    private final String maskedCardNumber;
    private final String imageUrl;
    // 추후 대표카드 설정 기능이 추가되면 같은 응답 구조를 재사용할 수 있다.
    private final Boolean representative;
}