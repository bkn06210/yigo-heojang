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
import com.wallet.card.mapper.CardMapper;
import com.wallet.card.support.CardBinFinder;
import com.wallet.card.support.CardNumberSupport;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;

@RequiredArgsConstructor
@Service
public class CardIdentificationService {
    private final CardMapper cardMapper;
    private final CardBinFinder cardBinFinder;

    @Transactional(readOnly = true)
    public CardCandidateListResponse findRegistrationCandidates(
        CardIdentificationRequest request
    ) {
        String normalizedCardNumber =
            CardNumberSupport.normalizeAndValidate(request.cardNumber());

        CardBin cardBin = cardBinFinder.findCardBin(normalizedCardNumber);

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