package com.wallet.card.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Card {
    private final Long cardId;
    private final Long cardCompanyId;
    private final String companyName;
    private final String cardName;
    private final String cardType;
    private final String imageUrl;
    private final String description;
}