package com.wallet.card.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserCard {
    private final Long userCardId;
    private final Long memberId;
    private final Long cardId;
    private final String maskedCardNumber;
    private final Boolean representative;
    private final CardStatus cardStatus;
}