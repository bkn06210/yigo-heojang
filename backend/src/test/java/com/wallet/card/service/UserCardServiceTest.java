package com.wallet.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import com.wallet.card.domain.Card;
import com.wallet.card.domain.CardBin;
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
import com.wallet.card.mapper.CardMapper;
import com.wallet.card.mapper.UserCardMapper;
import com.wallet.card.support.CardBinFinder;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;
import com.wallet.member.mapper.MemberMapper;

class UserCardServiceTest {
    private CardMapper cardMapper;
    private UserCardMapper userCardMapper;
    private CardBinFinder cardBinFinder;
    private MemberMapper memberMapper;
    private UserCardService userCardService;

    @BeforeEach
    void setUp() {
        cardMapper = mock(CardMapper.class);
        userCardMapper = mock(UserCardMapper.class);
        cardBinFinder = mock(CardBinFinder.class);
        memberMapper = mock(MemberMapper.class);

        userCardService = new UserCardService(
            cardMapper,
            userCardMapper,
            cardBinFinder,
            memberMapper
        );
    }

    @Test
    @DisplayName("보유 카드 등록 성공 - 기존 보유 카드가 없으면 신규 등록한다")
    void registerUserCard_success_insert() {
        // given
        Long memberId = 1L;
        Long cardId = 10L;

        UserCardRegisterRequest request =
            new UserCardRegisterRequest(cardId, "1234-5678-0000-0006");

        Card card = new Card(
            cardId,
            100L,
            "KB국민카드",
            "KB 국민 나라사랑카드",
            "CHECK",
            "https://example.com/kb.png",
            "생활 혜택 체크카드"
        );

        CardBin cardBin = new CardBin(
            1L,
            100L,
            "12345678",
            8
        );

        UserCardRegistrationResult result =
            new UserCardRegistrationResult(
                50L,
                "KB 국민 나라사랑카드",
                "KB국민카드",
                "****-****-****-0006",
                "https://example.com/kb.png",
                false
            );

        when(cardMapper.findActiveById(cardId))
            .thenReturn(card);

        when(cardBinFinder.findCardBin("1234567800000006"))
            .thenReturn(cardBin);

        when(userCardMapper.findByMemberIdAndCardId(memberId, cardId))
            .thenReturn(null);

        when(userCardMapper.findRegistrationResult(memberId, cardId))
            .thenReturn(result);

        // when
        UserCardRegisterResponse response =
            userCardService.registerUserCard(memberId, request);

        // then
        assertThat(response.userCardId()).isEqualTo(50L);
        assertThat(response.cardName()).isEqualTo("KB 국민 나라사랑카드");
        assertThat(response.issuerName()).isEqualTo("KB국민카드");
        assertThat(response.maskedCardNumber()).isEqualTo("****-****-****-0006");
        assertThat(response.imageUrl()).isEqualTo("https://example.com/kb.png");
        assertThat(response.representative()).isFalse();

        verify(userCardMapper)
            .insertUserCard(memberId, cardId, "****-****-****-0006");

        verify(userCardMapper, never())
            .reactivateUserCard(50L, memberId, "****-****-****-0006");
    }

    @Test
    @DisplayName("보유 카드 등록 성공 - 기존 보유 카드가 DELETED 상태면 재활성화한다")
    void registerUserCard_success_reactivateDeletedCard() {
        // given
        Long memberId = 1L;
        Long cardId = 10L;

        UserCardRegisterRequest request =
            new UserCardRegisterRequest(cardId, "1234-5678-0000-0006");

        Card card = new Card(
            cardId,
            100L,
            "KB국민카드",
            "KB 국민 나라사랑카드",
            "CHECK",
            "https://example.com/kb.png",
            "생활 혜택 체크카드"
        );

        CardBin cardBin = new CardBin(
            1L,
            100L,
            "12345678",
            8
        );

        UserCard deletedUserCard = new UserCard(
            50L,
            memberId,
            cardId,
            "****-****-****-1111",
            false,
            UserCardStatus.DELETED
        );

        UserCardRegistrationResult result =
            new UserCardRegistrationResult(
                50L,
                "KB 국민 나라사랑카드",
                "KB국민카드",
                "****-****-****-0006",
                "https://example.com/kb.png",
                false
            );

        when(cardMapper.findActiveById(cardId))
            .thenReturn(card);

        when(cardBinFinder.findCardBin("1234567800000006"))
            .thenReturn(cardBin);

        when(userCardMapper.findByMemberIdAndCardId(memberId, cardId))
            .thenReturn(deletedUserCard);

        when(userCardMapper.reactivateUserCard(
            50L,
            memberId,
            "****-****-****-0006"
        )).thenReturn(1);

        when(userCardMapper.findRegistrationResult(memberId, cardId))
            .thenReturn(result);

        // when
        UserCardRegisterResponse response =
            userCardService.registerUserCard(memberId, request);

        // then
        assertThat(response.userCardId()).isEqualTo(50L);
        assertThat(response.maskedCardNumber()).isEqualTo("****-****-****-0006");

        verify(userCardMapper, never())
            .insertUserCard(memberId, cardId, "****-****-****-0006");

        verify(userCardMapper)
            .reactivateUserCard(50L, memberId, "****-****-****-0006");
    }

    @Test
    @DisplayName("보유 카드 등록 실패 - 카드번호 형식이 유효하지 않으면 예외가 발생한다")
    void registerUserCard_fail_invalidCardNumber() {
        // given
        Long memberId = 1L;

        UserCardRegisterRequest request =
            new UserCardRegisterRequest(10L, "1234-5678-0000-0001");

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userCardService.registerUserCard(memberId, request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.CARD_NUMBER_INVALID);

        verify(cardMapper, never()).findActiveById(10L);
        verify(cardBinFinder, never()).findCardBin("1234567800000006");
        verify(userCardMapper, never()).findByMemberIdAndCardId(memberId, 10L);
    }

    @Test
    @DisplayName("보유 카드 등록 실패 - 선택한 카드 상품이 존재하지 않으면 예외가 발생한다")
    void registerUserCard_fail_cardNotFound() {
        // given
        Long memberId = 1L;
        Long cardId = 10L;

        UserCardRegisterRequest request =
            new UserCardRegisterRequest(cardId, "1234-5678-0000-0006");

        when(cardMapper.findActiveById(cardId))
            .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userCardService.registerUserCard(memberId, request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.CARD_NOT_FOUND);

        verify(cardMapper).findActiveById(cardId);
        verify(cardBinFinder, never()).findCardBin("1234567800000006");
        verify(userCardMapper, never()).findByMemberIdAndCardId(memberId, cardId);
    }

    @Test
    @DisplayName("보유 카드 등록 실패 - BIN 데이터가 없으면 예외가 발생한다")
    void registerUserCard_fail_binNotFound() {
        // given
        Long memberId = 1L;
        Long cardId = 10L;

        UserCardRegisterRequest request =
            new UserCardRegisterRequest(cardId, "1234-5678-0000-0006");

        Card card = new Card(
            cardId,
            100L,
            "KB국민카드",
            "KB 국민 나라사랑카드",
            "CHECK",
            "https://example.com/kb.png",
            "생활 혜택 체크카드"
        );

        when(cardMapper.findActiveById(cardId))
            .thenReturn(card);

        when(cardBinFinder.findCardBin("1234567800000006"))
            .thenThrow(new BusinessException(ErrorCode.CARD_BIN_NOT_FOUND));

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userCardService.registerUserCard(memberId, request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.CARD_BIN_NOT_FOUND);

        verify(cardBinFinder).findCardBin("1234567800000006");
        verify(userCardMapper, never()).findByMemberIdAndCardId(memberId, cardId);
    }

    @Test
    @DisplayName("보유 카드 등록 실패 - BIN 카드사와 선택 카드 상품의 카드사가 다르면 예외가 발생한다")
    void registerUserCard_fail_cardCompanyMismatch() {
        // given
        Long memberId = 1L;
        Long cardId = 10L;

        UserCardRegisterRequest request =
            new UserCardRegisterRequest(cardId, "1234-5678-0000-0006");

        Card selectedCard = new Card(
            cardId,
            100L,
            "KB국민카드",
            "KB 국민 나라사랑카드",
            "CHECK",
            "https://example.com/kb.png",
            "생활 혜택 체크카드"
        );

        CardBin cardBin = new CardBin(
            1L,
            200L,
            "12345678",
            8
        );

        when(cardMapper.findActiveById(cardId))
            .thenReturn(selectedCard);

        when(cardBinFinder.findCardBin("1234567800000006"))
            .thenReturn(cardBin);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userCardService.registerUserCard(memberId, request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.CARD_COMPANY_MISMATCH);

        verify(userCardMapper, never()).findByMemberIdAndCardId(memberId, cardId);
        verify(userCardMapper, never())
            .insertUserCard(memberId, cardId, "****-****-****-0006");
    }

    @Test
    @DisplayName("보유 카드 등록 실패 - 이미 ACTIVE 상태로 등록된 카드면 예외가 발생한다")
    void registerUserCard_fail_alreadyActiveUserCard() {
        // given
        Long memberId = 1L;
        Long cardId = 10L;

        UserCardRegisterRequest request =
            new UserCardRegisterRequest(cardId, "1234-5678-0000-0006");

        Card card = new Card(
            cardId,
            100L,
            "KB국민카드",
            "KB 국민 나라사랑카드",
            "CHECK",
            "https://example.com/kb.png",
            "생활 혜택 체크카드"
        );

        CardBin cardBin = new CardBin(
            1L,
            100L,
            "12345678",
            8
        );

        UserCard activeUserCard = new UserCard(
            50L,
            memberId,
            cardId,
            "****-****-****-0006",
            false,
            UserCardStatus.ACTIVE
        );

        when(cardMapper.findActiveById(cardId))
            .thenReturn(card);

        when(cardBinFinder.findCardBin("1234567800000006"))
            .thenReturn(cardBin);

        when(userCardMapper.findByMemberIdAndCardId(memberId, cardId))
            .thenReturn(activeUserCard);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userCardService.registerUserCard(memberId, request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.USER_CARD_ALREADY_EXISTS);

        verify(userCardMapper, never())
            .insertUserCard(memberId, cardId, "****-****-****-0006");

        verify(userCardMapper, never())
            .reactivateUserCard(50L, memberId, "****-****-****-0006");
    }

    @Test
    @DisplayName("보유 카드 등록 실패 - 동시 등록으로 unique key 충돌이 발생하면 중복 등록 예외로 변환한다")
    void registerUserCard_fail_duplicateKey() {
        // given
        Long memberId = 1L;
        Long cardId = 10L;

        UserCardRegisterRequest request =
            new UserCardRegisterRequest(cardId, "1234-5678-0000-0006");

        Card card = new Card(
            cardId,
            100L,
            "KB국민카드",
            "KB 국민 나라사랑카드",
            "CHECK",
            "https://example.com/kb.png",
            "생활 혜택 체크카드"
        );

        CardBin cardBin = new CardBin(
            1L,
            100L,
            "12345678",
            8
        );

        when(cardMapper.findActiveById(cardId))
            .thenReturn(card);

        when(cardBinFinder.findCardBin("1234567800000006"))
            .thenReturn(cardBin);

        when(userCardMapper.findByMemberIdAndCardId(memberId, cardId))
            .thenReturn(null);

        when(userCardMapper.insertUserCard(
            memberId,
            cardId,
            "****-****-****-0006"
        )).thenThrow(new DuplicateKeyException("duplicate"));

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userCardService.registerUserCard(memberId, request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.USER_CARD_ALREADY_EXISTS);

        verify(userCardMapper)
            .insertUserCard(memberId, cardId, "****-****-****-0006");

        verify(userCardMapper, never())
            .findRegistrationResult(memberId, cardId);
    }

    @Test
    @DisplayName("보유 카드 등록 실패 - 재활성화 update count가 1이 아니면 예외가 발생한다")
    void registerUserCard_fail_reactivateFailed() {
        // given
        Long memberId = 1L;
        Long cardId = 10L;

        UserCardRegisterRequest request =
            new UserCardRegisterRequest(cardId, "1234-5678-0000-0006");

        Card card = new Card(
            cardId,
            100L,
            "KB국민카드",
            "KB 국민 나라사랑카드",
            "CHECK",
            "https://example.com/kb.png",
            "생활 혜택 체크카드"
        );

        CardBin cardBin = new CardBin(
            1L,
            100L,
            "12345678",
            8
        );

        UserCard deletedUserCard = new UserCard(
            50L,
            memberId,
            cardId,
            "****-****-****-1111",
            false,
            UserCardStatus.DELETED
        );

        when(cardMapper.findActiveById(cardId))
            .thenReturn(card);

        when(cardBinFinder.findCardBin("1234567800000006"))
            .thenReturn(cardBin);

        when(userCardMapper.findByMemberIdAndCardId(memberId, cardId))
            .thenReturn(deletedUserCard);

        when(userCardMapper.reactivateUserCard(
            50L,
            memberId,
            "****-****-****-0006"
        )).thenReturn(0);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userCardService.registerUserCard(memberId, request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.USER_CARD_REGISTRATION_FAILED);

        verify(userCardMapper, never())
            .findRegistrationResult(memberId, cardId);
    }

    @Test
    @DisplayName("보유 카드 등록 실패 - 등록 후 결과 조회가 되지 않으면 예외가 발생한다")
    void registerUserCard_fail_registrationResultNotFound() {
        // given
        Long memberId = 1L;
        Long cardId = 10L;

        UserCardRegisterRequest request =
            new UserCardRegisterRequest(cardId, "1234-5678-0000-0006");

        Card card = new Card(
            cardId,
            100L,
            "KB국민카드",
            "KB 국민 나라사랑카드",
            "CHECK",
            "https://example.com/kb.png",
            "생활 혜택 체크카드"
        );

        CardBin cardBin = new CardBin(
            1L,
            100L,
            "12345678",
            8
        );

        when(cardMapper.findActiveById(cardId))
            .thenReturn(card);

        when(cardBinFinder.findCardBin("1234567800000006"))
            .thenReturn(cardBin);

        when(userCardMapper.findByMemberIdAndCardId(memberId, cardId))
            .thenReturn(null);

        when(userCardMapper.findRegistrationResult(memberId, cardId))
            .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userCardService.registerUserCard(memberId, request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.USER_CARD_REGISTRATION_FAILED);

        verify(userCardMapper)
            .insertUserCard(memberId, cardId, "****-****-****-0006");

        verify(userCardMapper)
            .findRegistrationResult(memberId, cardId);
    }

    @Test
    @DisplayName("보유 카드 목록 조회 성공 - 활성 카드 목록과 전체 개수를 반환한다")
    void getUserCards_success() {
        // given
        Long memberId = 1L;

        LocalDateTime firstRegisteredAt =
            LocalDateTime.of(2026, 8, 4, 15, 30);

        LocalDateTime secondRegisteredAt =
            LocalDateTime.of(2026, 8, 3, 11, 20);

        UserCardListResult firstResult = new UserCardListResult(
            50L,
            10L,
            "KB 국민 나라사랑카드",
            "KB국민카드",
            "CHECK",
            "****-****-****-0006",
            "https://example.com/kb.png",
            true,
            firstRegisteredAt
        );

        UserCardListResult secondResult = new UserCardListResult(
            51L,
            11L,
            "신한카드 Mr.Life",
            "신한카드",
            "CREDIT",
            "****-****-****-1234",
            "https://example.com/shinhan.png",
            false,
            secondRegisteredAt
        );

        when(userCardMapper.findActiveUserCardsByMemberId(memberId))
            .thenReturn(List.of(firstResult, secondResult));

        // when
        UserCardListResponse response =
            userCardService.getUserCards(memberId);

        // then
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.userCards()).hasSize(2);

        UserCardListItemResponse firstCard =
            response.userCards().get(0);

        assertThat(firstCard.userCardId()).isEqualTo(50L);
        assertThat(firstCard.cardId()).isEqualTo(10L);
        assertThat(firstCard.cardName())
            .isEqualTo("KB 국민 나라사랑카드");
        assertThat(firstCard.issuerName()).isEqualTo("KB국민카드");
        assertThat(firstCard.cardType()).isEqualTo("CHECK");
        assertThat(firstCard.maskedCardNumber())
            .isEqualTo("****-****-****-0006");
        assertThat(firstCard.imageUrl())
            .isEqualTo("https://example.com/kb.png");
        assertThat(firstCard.representative()).isTrue();
        assertThat(firstCard.registeredAt())
            .isEqualTo(firstRegisteredAt);

        UserCardListItemResponse secondCard =
            response.userCards().get(1);

        assertThat(secondCard.userCardId()).isEqualTo(51L);
        assertThat(secondCard.cardName())
            .isEqualTo("신한카드 Mr.Life");
        assertThat(secondCard.representative()).isFalse();
        assertThat(secondCard.registeredAt())
            .isEqualTo(secondRegisteredAt);

        verify(userCardMapper)
            .findActiveUserCardsByMemberId(memberId);
    }

    @Test
    @DisplayName("대표 카드 목록 조회 성공 - 로그인 회원의 활성 대표 카드 목록을 반환한다")
    void getRepresentativeUserCards_success() {
        // given
        Long memberId = 1L;

        List<UserCardListResult> representativeUserCards = List.of(
            new UserCardListResult(
                10L,
                100L,
                "KB국민 My WE:SH 카드",
                "KB국민카드",
                "CREDIT",
                "****-****-****-1234",
                "https://example.com/card-image-1.png",
                true,
                LocalDateTime.of(2026, 8, 6, 10, 0)
            ),
            new UserCardListResult(
                11L,
                101L,
                "신한 Deep Dream 카드",
                "신한카드",
                "CREDIT",
                "****-****-****-5678",
                "https://example.com/card-image-2.png",
                true,
                LocalDateTime.of(2026, 8, 6, 11, 0)
            )
        );

        when(userCardMapper.findActiveRepresentativeUserCardsByMemberId(memberId))
            .thenReturn(representativeUserCards);

        // when
        UserCardListResponse response =
            userCardService.getRepresentativeUserCards(memberId);

        // then
        assertThat(response.userCards()).hasSize(2);
        assertThat(response.userCards())
            .extracting(UserCardListItemResponse::representative)
            .containsOnly(true);

        verify(userCardMapper)
            .findActiveRepresentativeUserCardsByMemberId(memberId);
    }

    @Test
    @DisplayName("대표 카드 목록 조회 성공 - 대표 카드가 없으면 빈 목록을 반환한다")
    void getRepresentativeUserCards_success_emptyList() {
        // given
        Long memberId = 1L;

        when(userCardMapper.findActiveRepresentativeUserCardsByMemberId(memberId))
            .thenReturn(List.of());

        // when
        UserCardListResponse response =
            userCardService.getRepresentativeUserCards(memberId);

        // then
        assertThat(response.userCards()).isEmpty();

        verify(userCardMapper)
            .findActiveRepresentativeUserCardsByMemberId(memberId);
    }

    @Test
    @DisplayName("보유 카드 목록 조회 성공 - 보유 카드가 없으면 빈 목록과 0을 반환한다")
    void getUserCards_success_emptyList() {
        // given
        Long memberId = 1L;

        when(userCardMapper.findActiveUserCardsByMemberId(memberId))
            .thenReturn(List.of());

        // when
        UserCardListResponse response =
            userCardService.getUserCards(memberId);

        // then
        assertThat(response.userCards()).isEmpty();
        assertThat(response.totalCount()).isZero();

        verify(userCardMapper)
            .findActiveUserCardsByMemberId(memberId);
    }

    @Test
    @DisplayName("보유 카드 목록 조회 시 로그인 회원 ID를 Mapper에 전달한다")
    void getUserCards_passAuthenticatedMemberId() {
        // given
        Long authenticatedMemberId = 25L;

        when(userCardMapper.findActiveUserCardsByMemberId(
            authenticatedMemberId
        )).thenReturn(List.of());

        // when
        userCardService.getUserCards(authenticatedMemberId);

        // then
        verify(userCardMapper)
            .findActiveUserCardsByMemberId(authenticatedMemberId);
    }

    @Test
    @DisplayName("보유 카드 상세 조회에 성공한다")
    void getUserCardDetail_success() {
        // given
        Long memberId = 1L;
        Long userCardId = 15L;

        LocalDateTime registeredAt = LocalDateTime.of(2026, 8, 5, 14, 30);

        UserCardDetailResult detailResult = new UserCardDetailResult(
            userCardId,
            10L,
            "신한카드 Mr.Life",
            "신한카드",
            "CREDIT",
            "****-****-****-1234",
            "https://example.com/cards/mr-life.png",
            false,
            registeredAt
        );

        when(userCardMapper.findActiveDetailByIdAndMemberId(memberId, userCardId))
            .thenReturn(detailResult);

        // when
        UserCardDetailResponse response =
            userCardService.getUserCardDetail(memberId, userCardId);

        // then
        assertThat(response.userCardId()).isEqualTo(userCardId);
        assertThat(response.cardId()).isEqualTo(10L);
        assertThat(response.cardName()).isEqualTo("신한카드 Mr.Life");
        assertThat(response.issuerName()).isEqualTo("신한카드");
        assertThat(response.cardType()).isEqualTo("CREDIT");
        assertThat(response.maskedCardNumber()).isEqualTo("****-****-****-1234");
        assertThat(response.imageUrl()).isEqualTo("https://example.com/cards/mr-life.png");
        assertThat(response.representative()).isFalse();
        assertThat(response.registeredAt()).isEqualTo(registeredAt);

        verify(userCardMapper).findActiveDetailByIdAndMemberId(memberId, userCardId);
    }

    @Test
    @DisplayName("보유 카드 상세 조회 시 조회 대상이 없으면 예외가 발생한다")
    void getUserCardDetail_notFound() {
        // given
        Long memberId = 1L;
        Long userCardId = 999L;

        /*
         * Mapper에서 null이 반환되는 경우는 다음을 모두 포함한다.
         * - 존재하지 않는 보유 카드
         * - 다른 회원의 보유 카드
         * - DELETED 상태의 보유 카드
         */
        when(userCardMapper.findActiveDetailByIdAndMemberId(memberId, userCardId))
            .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userCardService.getUserCardDetail(memberId, userCardId)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.USER_CARD_NOT_FOUND);

        verify(userCardMapper).findActiveDetailByIdAndMemberId(memberId, userCardId);
    }

    @Test
    @DisplayName("보유 카드 삭제 성공 - 본인이 소유한 활성 카드를 삭제한다")
    void deleteUserCard_success() {
        // given
        Long memberId = 1L;
        Long userCardId = 50L;

        when(userCardMapper.softDeleteByIdAndMemberId(
            userCardId,
            memberId
        )).thenReturn(1);

        // when
        userCardService.deleteUserCard(memberId, userCardId);

        // then
        verify(userCardMapper)
            .softDeleteByIdAndMemberId(userCardId, memberId);
    }

    @Test
    @DisplayName("보유 카드 삭제 실패 - 삭제할 수 있는 활성 카드를 찾지 못하면 예외가 발생한다")
    void deleteUserCard_fail_userCardNotFound() {
        // given
        Long memberId = 1L;
        Long userCardId = 50L;

        when(userCardMapper.softDeleteByIdAndMemberId(
            userCardId,
            memberId
        )).thenReturn(0);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userCardService.deleteUserCard(memberId, userCardId)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.USER_CARD_NOT_FOUND);

        verify(userCardMapper)
            .softDeleteByIdAndMemberId(userCardId, memberId);
    }
    @Test
    @DisplayName("대표 카드 설정 성공 - 대표 카드가 3개 미만이면 일반 카드를 대표 카드로 설정한다")
    void updateRepresentative_success_setRepresentative() {
        // given
        Long memberId = 1L;
        Long userCardId = 50L;

        UserCard userCard = new UserCard(
            userCardId,
            memberId,
            10L,
            "****-****-****-0006",
            false,
            UserCardStatus.ACTIVE
        );

        when(memberMapper.lockActiveMemberById(memberId))
            .thenReturn(memberId);

        when(userCardMapper.findActiveByIdAndMemberId(memberId, userCardId))
            .thenReturn(userCard);

        when(userCardMapper.countActiveRepresentativeCards(memberId))
            .thenReturn(2);

        when(userCardMapper.updateRepresentative(
            memberId,
            userCardId,
            true
        )).thenReturn(1);

        // when
        userCardService.updateRepresentative(memberId, userCardId, true);

        // then
        verify(memberMapper).lockActiveMemberById(memberId);
        verify(userCardMapper).findActiveByIdAndMemberId(memberId, userCardId);
        verify(userCardMapper).countActiveRepresentativeCards(memberId);
        verify(userCardMapper).updateRepresentative(memberId, userCardId, true);
    }

    @Test
    @DisplayName("대표 카드 설정 실패 - 이미 대표 카드가 3개이면 추가 설정할 수 없다")
    void updateRepresentative_fail_representativeLimitExceeded() {
        // given
        Long memberId = 1L;
        Long userCardId = 50L;

        UserCard userCard = new UserCard(
            userCardId,
            memberId,
            10L,
            "****-****-****-0006",
            false,
            UserCardStatus.ACTIVE
        );

        when(memberMapper.lockActiveMemberById(memberId))
            .thenReturn(memberId);

        when(userCardMapper.findActiveByIdAndMemberId(memberId, userCardId))
            .thenReturn(userCard);

        when(userCardMapper.countActiveRepresentativeCards(memberId))
            .thenReturn(3);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userCardService.updateRepresentative(memberId, userCardId, true)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.REPRESENTATIVE_CARD_LIMIT_EXCEEDED);

        verify(userCardMapper, never())
            .updateRepresentative(memberId, userCardId, true);
    }

    @Test
    @DisplayName("대표 카드 설정 성공 - 이미 대표 카드이면 변경 없이 성공 처리한다")
    void updateRepresentative_success_alreadyRepresentative() {
        // given
        Long memberId = 1L;
        Long userCardId = 50L;

        UserCard userCard = new UserCard(
            userCardId,
            memberId,
            10L,
            "****-****-****-0006",
            true,
            UserCardStatus.ACTIVE
        );

        when(memberMapper.lockActiveMemberById(memberId))
            .thenReturn(memberId);

        when(userCardMapper.findActiveByIdAndMemberId(memberId, userCardId))
            .thenReturn(userCard);

        // when
        userCardService.updateRepresentative(memberId, userCardId, true);

        // then
        verify(userCardMapper, never())
            .countActiveRepresentativeCards(memberId);

        verify(userCardMapper, never())
            .updateRepresentative(memberId, userCardId, true);
    }

    @Test
    @DisplayName("대표 카드 해제 성공 - 대표 카드를 일반 카드로 변경한다")
    void updateRepresentative_success_unsetRepresentative() {
        // given
        Long memberId = 1L;
        Long userCardId = 50L;

        UserCard userCard = new UserCard(
            userCardId,
            memberId,
            10L,
            "****-****-****-0006",
            true,
            UserCardStatus.ACTIVE
        );

        when(memberMapper.lockActiveMemberById(memberId))
            .thenReturn(memberId);

        when(userCardMapper.findActiveByIdAndMemberId(memberId, userCardId))
            .thenReturn(userCard);

        when(userCardMapper.updateRepresentative(
            memberId,
            userCardId,
            false
        )).thenReturn(1);

        // when
        userCardService.updateRepresentative(memberId, userCardId, false);

        // then
        verify(userCardMapper, never())
            .countActiveRepresentativeCards(memberId);

        verify(userCardMapper)
            .updateRepresentative(memberId, userCardId, false);
    }

    @Test
    @DisplayName("대표 카드 해제 성공 - 이미 일반 카드이면 변경 없이 성공 처리한다")
    void updateRepresentative_success_alreadyNotRepresentative() {
        // given
        Long memberId = 1L;
        Long userCardId = 50L;

        UserCard userCard = new UserCard(
            userCardId,
            memberId,
            10L,
            "****-****-****-0006",
            false,
            UserCardStatus.ACTIVE
        );

        when(memberMapper.lockActiveMemberById(memberId))
            .thenReturn(memberId);

        when(userCardMapper.findActiveByIdAndMemberId(memberId, userCardId))
            .thenReturn(userCard);

        // when
        userCardService.updateRepresentative(memberId, userCardId, false);

        // then
        verify(userCardMapper, never())
            .countActiveRepresentativeCards(memberId);

        verify(userCardMapper, never())
            .updateRepresentative(memberId, userCardId, false);
    }

    @Test
    @DisplayName("대표 카드 변경 실패 - 로그인 회원의 활성 보유 카드를 찾지 못하면 예외가 발생한다")
    void updateRepresentative_fail_userCardNotFound() {
        // given
        Long memberId = 1L;
        Long userCardId = 999L;

        when(memberMapper.lockActiveMemberById(memberId))
            .thenReturn(memberId);

        when(userCardMapper.findActiveByIdAndMemberId(memberId, userCardId))
            .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userCardService.updateRepresentative(memberId, userCardId, true)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.USER_CARD_NOT_FOUND);

        verify(userCardMapper, never())
            .countActiveRepresentativeCards(memberId);

        verify(userCardMapper, never())
            .updateRepresentative(memberId, userCardId, true);
    }

    @Test
    @DisplayName("대표 카드 변경 실패 - 활성 회원을 찾지 못하면 예외가 발생한다")
    void updateRepresentative_fail_memberNotFound() {
        // given
        Long memberId = 1L;
        Long userCardId = 50L;

        when(memberMapper.lockActiveMemberById(memberId))
            .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userCardService.updateRepresentative(memberId, userCardId, true)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

        verify(userCardMapper, never())
            .findActiveByIdAndMemberId(memberId, userCardId);

        verify(userCardMapper, never())
            .countActiveRepresentativeCards(memberId);

        verify(userCardMapper, never())
            .updateRepresentative(memberId, userCardId, true);
    }

    @Test
    @DisplayName("대표 카드 변경 실패 - 대표 카드 상태 변경 결과가 1건이 아니면 예외가 발생한다")
    void updateRepresentative_fail_updateCountNotOne() {
        // given
        Long memberId = 1L;
        Long userCardId = 50L;

        UserCard userCard = new UserCard(
            userCardId,
            memberId,
            10L,
            "****-****-****-0006",
            false,
            UserCardStatus.ACTIVE
        );

        when(memberMapper.lockActiveMemberById(memberId))
            .thenReturn(memberId);

        when(userCardMapper.findActiveByIdAndMemberId(memberId, userCardId))
            .thenReturn(userCard);

        when(userCardMapper.countActiveRepresentativeCards(memberId))
            .thenReturn(2);

        when(userCardMapper.updateRepresentative(
            memberId,
            userCardId,
            true
        )).thenReturn(0);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> userCardService.updateRepresentative(memberId, userCardId, true)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.USER_CARD_NOT_FOUND);

        verify(userCardMapper)
            .updateRepresentative(memberId, userCardId, true);
    }

}
