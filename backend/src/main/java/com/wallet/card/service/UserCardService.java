package com.wallet.card.service;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.card.domain.MockCard;
import com.wallet.card.domain.UserCard;
import com.wallet.card.domain.UserCardDetailResult;
import com.wallet.card.domain.UserCardListResult;
import com.wallet.card.domain.UserCardRegistrationResult;
import com.wallet.card.domain.UserCardStatus;
import com.wallet.card.dto.UserCardDetailResponse;
import com.wallet.card.dto.UserCardListItemResponse;
import com.wallet.card.dto.UserCardListResponse;
import com.wallet.card.dto.UserCardRegisterRequest;
import com.wallet.card.dto.UserCardRegisterResponse;
import com.wallet.card.mapper.MockCardMapper;
import com.wallet.card.mapper.UserCardMapper;
import com.wallet.card.support.CardMaskingSupport;
import com.wallet.card.support.CardNumberSupport;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.mapper.MemberMapper;

@RequiredArgsConstructor
@Service
public class UserCardService {
    private static final int MAX_REPRESENTATIVE_CARD_COUNT = 3;

    private final MockCardMapper mockCardMapper;
    private final UserCardMapper userCardMapper;
    private final MemberMapper memberMapper;

    @Transactional
    public UserCardRegisterResponse registerUserCard(
        Long memberId,
        UserCardRegisterRequest request
    ) {
        String normalizedCardNumber =
            CardNumberSupport.normalizeAndValidate(request.cardNumber());

        // 클라이언트가 cardId를 정하지 못하게 하고, 서버가 신뢰하는 Mock 매핑에서 카드 상품을 결정한다.
        MockCard mockCard = findSupportedMockCard(normalizedCardNumber);
        Long cardId = mockCard.getCardId();

        String maskedCardNumber =
            CardMaskingSupport.mask(normalizedCardNumber);

        UserCard existingUserCard =
            userCardMapper.findByMemberIdAndCardId(memberId, cardId);

        if (existingUserCard == null) {
            insertUserCard(memberId, cardId, maskedCardNumber);
        } else {
            registerExistingUserCard(memberId, existingUserCard, maskedCardNumber);
        }

        UserCardRegistrationResult result =
            userCardMapper.findRegistrationResult(memberId, cardId);

        if (result == null) {
            throw new BusinessException(ErrorCode.USER_CARD_REGISTRATION_FAILED);
        }

        return toResponse(result);
    }

    /**
     * 로그인 회원이 대표 카드로 설정한 활성 보유 카드 목록을 조회한다.
     * <p>
     * 대표 카드 목록도 기존 보유 카드 목록과 응답 구조가 같다.
     * 차이는 전체 활성 보유 카드가 아니라,
     * is_representative = 1인 카드만 조회한다는 점이다.
     */
    @Transactional(readOnly = true)
    public UserCardListResponse getRepresentativeUserCards(Long memberId) {
        List<UserCardListResult> results =
            userCardMapper.findActiveRepresentativeUserCardsByMemberId(memberId);

        List<UserCardListItemResponse> representativeUserCards = results.stream()
            .map(this::toListItemResponse)
            .toList();

        return UserCardListResponse.from(representativeUserCards);
    }

    // 로그인 회원이 보유한 활성 카드 목록을 조회한다.
    @Transactional(readOnly = true)
    public UserCardListResponse getUserCards(Long memberId) {
        List<UserCardListResult> results =
            userCardMapper.findActiveUserCardsByMemberId(memberId);

        List<UserCardListItemResponse> userCards = results.stream()
            .map(this::toListItemResponse)
            .toList();

        return UserCardListResponse.from(userCards);
    }

    // 로그인한 회원이 보유한 카드의 상세 기본 정보를 조회한다.
    @Transactional(readOnly = true)
    public UserCardDetailResponse getUserCardDetail(Long memberId, Long userCardId) {
        UserCardDetailResult detailResult =
            userCardMapper.findActiveDetailByIdAndMemberId(memberId, userCardId);

        if (detailResult == null) {
            throw new BusinessException(ErrorCode.USER_CARD_NOT_FOUND);
        }

        return UserCardDetailResponse.from(detailResult);
    }

    // 보유 카드의 대표 카드 여부를 요청한 최종 상태로 변경한다.
    @Transactional
    public void updateRepresentative(
        Long memberId,
        Long userCardId,
        boolean representative
    ) {
        lockActiveMember(memberId);

        UserCard userCard =
            userCardMapper.findActiveByIdAndMemberId(memberId, userCardId);
        if (userCard == null) {
            throw new BusinessException(ErrorCode.USER_CARD_NOT_FOUND);
        }

        if (isSameRepresentativeStatus(userCard, representative)) {
            return;
        }

        if (representative) {
            validateRepresentativeCardLimit(memberId);
        }

        int updatedCount = userCardMapper.updateRepresentative(
            memberId,
            userCardId,
            representative
        );

        if (updatedCount != 1) {
            throw new BusinessException(ErrorCode.USER_CARD_NOT_FOUND);
        }
    }

    // 로그인 회원이 소유한 활성 보유 카드를 삭제 상태로 변경한다.
    @Transactional
    public void deleteUserCard(Long memberId, Long userCardId) {
        int updatedCount =
            userCardMapper.softDeleteByIdAndMemberId(userCardId, memberId);

        if (updatedCount != 1) {
            throw new BusinessException(ErrorCode.USER_CARD_NOT_FOUND);
        }
    }

    private void lockActiveMember(Long memberId) {
        Long lockedMemberId = memberMapper.lockActiveMemberById(memberId);

        if (lockedMemberId == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private boolean isSameRepresentativeStatus(
        UserCard userCard,
        boolean representative
    ) {
        return Boolean.valueOf(representative)
            .equals(userCard.getRepresentative());
    }

    private void validateRepresentativeCardLimit(Long memberId) {
        int representativeCardCount =
            userCardMapper.countActiveRepresentativeCards(memberId);

        if (representativeCardCount >= MAX_REPRESENTATIVE_CARD_COUNT) {
            throw new BusinessException(
                ErrorCode.REPRESENTATIVE_CARD_LIMIT_EXCEEDED
            );
        }
    }

    private MockCard findSupportedMockCard(String normalizedCardNumber) {
        MockCard mockCard =
            mockCardMapper.findActiveByCardNumber(normalizedCardNumber);

        if (mockCard == null) {
            throw new BusinessException(ErrorCode.CARD_NOT_SUPPORTED);
        }

        return mockCard;
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
        if ((existingUserCard.getStatus()) == UserCardStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_CARD_ALREADY_EXISTS);
        }

        if (existingUserCard.getStatus() == UserCardStatus.DELETED) {
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
            result.getCardId(),
            result.getCardName(),
            result.getCompanyName(),
            result.getCardType(),
            result.getMaskedCardNumber(),
            result.getImageUrl(),
            result.getRepresentative()
        );
    }

    private UserCardListItemResponse toListItemResponse(
        UserCardListResult result
    ) {
        return new UserCardListItemResponse(
            result.getUserCardId(),
            result.getCardId(),
            result.getCardName(),
            result.getIssuerName(),
            result.getCardType(),
            result.getMaskedCardNumber(),
            result.getImageUrl(),
            result.getRepresentative(),
            result.getRegisteredAt()
        );
    }
}
