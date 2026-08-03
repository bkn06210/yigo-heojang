package com.wallet.card.service;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.card.domain.Card;
import com.wallet.card.domain.CardBin;
import com.wallet.card.dto.CardCandidateListResponse;
import com.wallet.card.dto.CardCandidateResponse;
import com.wallet.card.dto.CardIdentificationRequest;
import com.wallet.card.mapper.CardBinMapper;
import com.wallet.card.mapper.CardMapper;
import com.wallet.card.support.CardNumberSupport;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;

@RequiredArgsConstructor
@Service
public class CardIdentificationService {
    private static final int EIGHT_DIGIT_BIN_LENGTH = 8;
    private static final int SIX_DIGIT_BIN_LENGTH = 6;

    private final CardBinMapper cardBinMapper;
    private final CardMapper cardMapper;

    @Transactional(readOnly = true)
    public CardCandidateListResponse findRegistrationCandidates(
        CardIdentificationRequest request
    ) {
        String normalizedCardNumber =
            CardNumberSupport.normalizeAndValidate(request.cardNumber());

        CardBin cardBin = findCardBin(normalizedCardNumber);

        List<Card> cards =
            cardMapper.findActiveCardsByCompanyId(cardBin.getCardCompanyId());

        if (cards.isEmpty()) {
            throw new BusinessException(ErrorCode.CARD_CANDIDATE_NOT_FOUND);
        }

        return new CardCandidateListResponse(
            cards.get(0).getCompanyName(),
            CardNumberSupport.extractLastFourDigits(normalizedCardNumber),
            toResponses(cards)
        );
    }

    private CardBin findCardBin(String normalizedCardNumber) {
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
        String firstEightDigits = normalizedCardNumber.substring(0, EIGHT_DIGIT_BIN_LENGTH);
        return cardBinMapper.findActiveByPrefix(firstEightDigits);
    }

    private CardBin findBySixDigitBin(String normalizedCardNumber) {
        String firstSixDigits = normalizedCardNumber.substring(0, SIX_DIGIT_BIN_LENGTH);
        return cardBinMapper.findActiveByPrefix(firstSixDigits);
    }

    private List<CardCandidateResponse> toResponses(List<Card> cards) {
        return cards.stream()
            .map(this::toResponse)
            .toList();
    }

    private CardCandidateResponse toResponse(Card card) {
        return new CardCandidateResponse(
            card.getCardId(),
            card.getCardName(),
            card.getCompanyName(),
            card.getCardType(),
            card.getImageUrl(),
            card.getDescription()
        );
    }
}