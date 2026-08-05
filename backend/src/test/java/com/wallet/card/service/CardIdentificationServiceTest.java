package com.wallet.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.card.domain.Card;
import com.wallet.card.domain.CardBin;
import com.wallet.card.dto.CardCandidateListResponse;
import com.wallet.card.dto.CardCandidateResponse;
import com.wallet.card.dto.CardIdentificationRequest;
import com.wallet.card.mapper.CardMapper;
import com.wallet.card.support.CardBinFinder;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;

class CardIdentificationServiceTest {
    private CardMapper cardMapper;
    private CardBinFinder cardBinFinder;
    private CardIdentificationService cardIdentificationService;

    @BeforeEach
    void setUp() {
        cardMapper = mock(CardMapper.class);
        cardBinFinder = mock(CardBinFinder.class);

        cardIdentificationService = new CardIdentificationService(
            cardMapper,
            cardBinFinder
        );
    }

    @Test
    @DisplayName("카드 상품 후보 조회 성공 - 8자리 BIN이 존재하면 8자리 BIN을 우선 사용한다")
    void findRegistrationCandidates_success_withEightDigitBin() {
        // given
        CardIdentificationRequest request =
            new CardIdentificationRequest("1234-5678-0000-0006");

        CardBin cardBin = new CardBin(
            1L,
            10L,
            "12345678",
            8
        );

        Card card = new Card(
            1L,
            10L,
            "KB국민카드",
            "KB 국민 나라사랑카드",
            "CHECK",
            "https://example.com/images/cards/kb-narasarang.png",
            "교통·편의점·외식 생활 혜택 중심 체크카드"
        );

        when(cardBinFinder.findCardBin("1234567800000006"))
            .thenReturn(cardBin);

        when(cardMapper.findActiveCardsByCompanyId(10L))
            .thenReturn(List.of(card));

        // when
        CardCandidateListResponse response =
            cardIdentificationService.findRegistrationCandidates(request);

        // then
        assertThat(response.issuerName()).isEqualTo("KB국민카드");
        assertThat(response.lastFourDigits()).isEqualTo("0006");
        assertThat(response.cards()).hasSize(1);

        CardCandidateResponse candidate = response.cards().get(0);
        assertThat(candidate.cardId()).isEqualTo(1L);
        assertThat(candidate.cardName()).isEqualTo("KB 국민 나라사랑카드");
        assertThat(candidate.issuerName()).isEqualTo("KB국민카드");
        assertThat(candidate.cardType()).isEqualTo("CHECK");
        assertThat(candidate.imageUrl())
            .isEqualTo("https://example.com/images/cards/kb-narasarang.png");
        assertThat(candidate.description())
            .isEqualTo("교통·편의점·외식 생활 혜택 중심 체크카드");

        verify(cardBinFinder).findCardBin("1234567800000006");
        verify(cardBinFinder, never()).findCardBin("12345600000006");
        verify(cardMapper).findActiveCardsByCompanyId(10L);
    }

    @Test
    @DisplayName("카드 상품 후보 조회 실패 - 카드번호가 룬 알고리즘을 통과하지 못하면 예외가 발생한다")
    void findRegistrationCandidates_fail_invalidCardNumber() {
        // given
        CardIdentificationRequest request =
            new CardIdentificationRequest("1234-5678-0000-0001");

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> cardIdentificationService.findRegistrationCandidates(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.CARD_NUMBER_INVALID);

        verify(cardBinFinder, never()).findCardBin("1234567800000006");
        verify(cardMapper, never()).findActiveCardsByCompanyId(10L);
    }

    @Test
    @DisplayName("카드 상품 후보 조회 실패 - BIN 데이터가 없으면 예외가 발생한다")
    void findRegistrationCandidates_fail_binNotFound() {
        // given
        CardIdentificationRequest request =
            new CardIdentificationRequest("1234-5678-0000-0006");

        when(cardBinFinder.findCardBin("1234567800000006"))
            .thenThrow(new BusinessException(ErrorCode.CARD_BIN_NOT_FOUND));

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> cardIdentificationService.findRegistrationCandidates(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.CARD_BIN_NOT_FOUND);

        verify(cardBinFinder).findCardBin("1234567800000006");
        verify(cardMapper, never()).findActiveCardsByCompanyId(10L);
    }

    @Test
    @DisplayName("카드 상품 후보 조회 실패 - 카드사에 등록 가능한 활성 카드 상품이 없으면 예외가 발생한다")
    void findRegistrationCandidates_fail_candidateNotFound() {
        // given
        CardIdentificationRequest request =
            new CardIdentificationRequest("1234-5678-0000-0006");

        CardBin cardBin = new CardBin(
            1L,
            10L,
            "12345678",
            8
        );

        when(cardBinFinder.findCardBin("1234567800000006"))
            .thenReturn(cardBin);

        when(cardMapper.findActiveCardsByCompanyId(10L))
            .thenReturn(List.of());

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> cardIdentificationService.findRegistrationCandidates(request)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.CARD_CANDIDATE_NOT_FOUND);

        verify(cardBinFinder).findCardBin("1234567800000006");
        verify(cardMapper).findActiveCardsByCompanyId(10L);
    }
}