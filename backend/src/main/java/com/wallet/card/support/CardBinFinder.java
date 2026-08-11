package com.wallet.card.support;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import com.wallet.card.domain.CardBin;
import com.wallet.card.mapper.CardBinMapper;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;

@Component
@RequiredArgsConstructor
public class CardBinFinder {
    private final CardBinMapper cardBinMapper;

    public CardBin findCardBin(String normalizedCardNumber) {
        CardBin cardBin = findByEightDigitBin(normalizedCardNumber);

        if (cardBin != null) {
            return cardBin;
        }

        cardBin = findBySixDigitBin(normalizedCardNumber);

        if (cardBin == null) {
            throw new BusinessException(ErrorCode.CARD_BIN_NOT_FOUND);
        }

        return cardBin;
    }

    private CardBin findByEightDigitBin(String normalizedCardNumber) {
        String prefix = normalizedCardNumber.substring(0, CardBinPolicy.EIGHT_DIGIT_BIN_LENGTH);
        return cardBinMapper.findActiveByPrefix(prefix);
    }

    private CardBin findBySixDigitBin(String normalizedCardNumber) {
        String prefix = normalizedCardNumber.substring(0, CardBinPolicy.SIX_DIGIT_BIN_LENGTH);
        return cardBinMapper.findActiveByPrefix(prefix);
    }
}
