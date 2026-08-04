package com.wallet.card.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.card.domain.CardBin;
import com.wallet.card.mapper.CardBinMapper;
import com.wallet.common.ErrorCode;
import com.wallet.common.exception.BusinessException;

class CardBinFinderTest {
    private CardBinMapper cardBinMapper;
    private CardBinFinder cardBinFinder;

    @BeforeEach
    void setUp() {
        cardBinMapper = mock(CardBinMapper.class);
        cardBinFinder = new CardBinFinder(cardBinMapper);
    }

    @Test
    @DisplayName("BIN 조회 성공 - 8자리 BIN이 존재하면 8자리 BIN을 사용한다")
    void findCardBin_success_withEightDigitBin() {
        // given
        String normalizedCardNumber = "1234567800000006";
        CardBin cardBin = new CardBin(1L, 100L, "12345678", 8);

        when(cardBinMapper.findActiveByPrefix("12345678"))
            .thenReturn(cardBin);

        // when
        CardBin result = cardBinFinder.findCardBin(normalizedCardNumber);

        // then
        assertThat(result).isEqualTo(cardBin);

        verify(cardBinMapper).findActiveByPrefix("12345678");
        verify(cardBinMapper, never()).findActiveByPrefix("123456");
    }

    @Test
    @DisplayName("BIN 조회 성공 - 8자리 BIN이 없으면 6자리 BIN으로 재조회한다")
    void findCardBin_success_withSixDigitFallback() {
        // given
        String normalizedCardNumber = "6543210000000006";
        CardBin cardBin = new CardBin(2L, 20L, "654321", 6);

        when(cardBinMapper.findActiveByPrefix("65432100"))
            .thenReturn(null);

        when(cardBinMapper.findActiveByPrefix("654321"))
            .thenReturn(cardBin);

        // when
        CardBin result = cardBinFinder.findCardBin(normalizedCardNumber);

        // then
        assertThat(result).isEqualTo(cardBin);

        verify(cardBinMapper).findActiveByPrefix("65432100");
        verify(cardBinMapper).findActiveByPrefix("654321");
    }

    @Test
    @DisplayName("BIN 조회 실패 - 8자리와 6자리 BIN이 모두 없으면 예외가 발생한다")
    void findCardBin_fail_binNotFound() {
        // given
        String normalizedCardNumber = "1234567800000006";

        when(cardBinMapper.findActiveByPrefix("12345678"))
            .thenReturn(null);

        when(cardBinMapper.findActiveByPrefix("123456"))
            .thenReturn(null);

        // when
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> cardBinFinder.findCardBin(normalizedCardNumber)
        );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.CARD_BIN_NOT_FOUND);

        verify(cardBinMapper).findActiveByPrefix("12345678");
        verify(cardBinMapper).findActiveByPrefix("123456");
    }
}