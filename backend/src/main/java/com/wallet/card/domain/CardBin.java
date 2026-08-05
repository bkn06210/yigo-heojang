package com.wallet.card.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CardBin {
    private final Long cardBinId;
    private final Long cardCompanyId;
    private final String binPrefix;
    private final Integer binLength;
}