package com.wallet.card.service;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.card.domain.Card;
import com.wallet.card.domain.CardBin;
import com.wallet.card.domain.CardStatus;
import com.wallet.card.domain.UserCard;
import com.wallet.card.domain.UserCardRegistrationResult;
import com.wallet.card.dto.UserCardRegisterRequest;
import com.wallet.card.dto.UserCardRegisterResponse;
import com.wallet.card.mapper.CardMapper;
import com.wallet.card.mapper.UserCardMapper;
import com.wallet.card.support.CardBinFinder;
import com.wallet.card.support.CardMaskingSupport;
import com.wallet.card.support.CardNumberSupport;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;

@RequiredArgsConstructor
@Service
public class UserCardService {
    private final CardMapper cardMapper;
    private final UserCardMapper userCardMapper;
    private final CardBinFinder cardBinFinder;

    @Transactional
    public UserCardRegisterResponse registerUserCard(
        Long memberId,
        UserCardRegisterRequest request
    ) {
        String normalizedCardNumber =
            CardNumberSupport.normalizeAndValidate(request.cardNumber());

        String maskedCardNumber =
            CardMaskingSupport.mask(normalizedCardNumber);

        Card selectedCard = findSelectedCard(request.cardId());

        CardBin cardBin = cardBinFinder.findCardBin(normalizedCardNumber);

        validateCardCompany(selectedCard, cardBin);

        UserCard existingUserCard =
            userCardMapper.findByMemberIdAndCardId(memberId, request.cardId());

        if (existingUserCard == null) {
            insertUserCard(memberId, request.cardId(), maskedCardNumber);
        } else {
            registerExistingUserCard(memberId, existingUserCard, maskedCardNumber);
        }

        UserCardRegistrationResult result =
            userCardMapper.findRegistrationResult(memberId, request.cardId());

        if (result == null) {
            throw new BusinessException(ErrorCode.USER_CARD_REGISTRATION_FAILED);
        }

        return toResponse(result);
    }

    private Card findSelectedCard(Long cardId) {
        Card card = cardMapper.findActiveById(cardId);

        if (card == null) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }

        return card;
    }

    private void validateCardCompany(Card selectedCard, CardBin cardBin) {
        if (!selectedCard.getCardCompanyId().equals(cardBin.getCardCompanyId())) {
            throw new BusinessException(ErrorCode.CARD_COMPANY_MISMATCH);
        }
    }

    private void insertUserCard(
        Long memberId,
        Long cardId,
        String maskedCardNumber
    ) {
        try {
            userCardMapper.insertUserCard(memberId, cardId, maskedCardNumber);
        } catch (DuplicateKeyException e) {
            /*
             * 같은 회원이 같은 카드를 동시에 등록하면
             * 둘 다 기존 카드 조회에서는 null을 받을 수 있다.
             *
             * 이 경우 DB의 unique key가 마지막 방어선이 되므로,
             * DuplicateKeyException을 사용자 친화적인 중복 등록 예외로 변환한다.
             */
            throw new BusinessException(ErrorCode.USER_CARD_ALREADY_EXISTS);
        }
    }

    private void registerExistingUserCard(
        Long memberId,
        UserCard existingUserCard,
        String maskedCardNumber
    ) {
        if ((existingUserCard.getCardStatus()) == CardStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_CARD_ALREADY_EXISTS);
        }

        if (existingUserCard.getCardStatus() == CardStatus.DELETED) {
            reactivateUserCard(memberId, existingUserCard, maskedCardNumber);
            return;
        }

        throw new BusinessException(ErrorCode.USER_CARD_REGISTRATION_FAILED);
    }

    private void reactivateUserCard(
        Long memberId,
        UserCard existingUserCard,
        String maskedCardNumber
    ) {
        int updatedCount = userCardMapper.reactivateUserCard(
            existingUserCard.getUserCardId(),
            memberId,
            maskedCardNumber
        );

        if (updatedCount != 1) {
            throw new BusinessException(ErrorCode.USER_CARD_REGISTRATION_FAILED);
        }
    }

    private UserCardRegisterResponse toResponse(UserCardRegistrationResult result) {
        return new UserCardRegisterResponse(
            result.getUserCardId(),
            result.getCardName(),
            result.getCompanyName(),
            result.getMaskedCardNumber(),
            result.getImageUrl(),
            result.getRepresentative()
        );
    }
}