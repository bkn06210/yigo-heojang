package com.wallet.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.auth.domain.RefreshToken;
import com.wallet.auth.mapper.RefreshTokenMapper;
import com.wallet.auth.support.TokenHashUtil;

public class RefreshTokenServiceTest {
    private RefreshTokenMapper refreshTokenMapper;
    private RefreshTokenService refreshTokenService;
    private TokenHashUtil tokenHashUtil;

    @BeforeEach
    void setUp() {
        refreshTokenMapper = mock(RefreshTokenMapper.class);
        tokenHashUtil = new TokenHashUtil();
        refreshTokenService = new RefreshTokenService(refreshTokenMapper, tokenHashUtil);
    }

    @Test
    @DisplayName("유효 Refresh Token 조회 - 원문 토큰을 해시로 변환해 조회한다")
    void findValidToken_hashesTokenAndFindsToken() {
        // given
        String refreshToken = "refresh.token.value";
        String expectedHash = tokenHashUtil.sha256(refreshToken);

        RefreshToken savedToken = new RefreshToken();

        when(refreshTokenMapper.findValidTokenByHash(expectedHash))
            .thenReturn(savedToken);

        // when
        RefreshToken result = refreshTokenService.findValidToken(refreshToken);

        // then
        assertThat(result).isEqualTo(savedToken);
        verify(refreshTokenMapper).findValidTokenByHash(expectedHash);
    }

    @Test
    @DisplayName("Refresh Token 폐기 - 원문 토큰을 해시로 변환해 LOGOUT 사유로 폐기한다")
    void revokeByToken_hashesTokenAndRevokesIt() {
        // given
        String refreshToken = "refresh.token.value";
        String expectedHash = tokenHashUtil.sha256(refreshToken);

        // when
        refreshTokenService.revokeByToken(refreshToken);

        // then
        verify(refreshTokenMapper).revokeByHash(
            expectedHash,
            RefreshToken.REVOKE_REASON_LOGOUT);
    }

    @Test
    @DisplayName("비밀번호 재설정으로 회원의 모든 Refresh Token을 폐기한다")
    void revokeAllByPasswordReset_revokesAllTokensByMemberId() {
        // given
        Long memberId = 1L;

        // when
        refreshTokenService.revokeAllByPasswordReset(memberId);

        // then
        verify(refreshTokenMapper).revokeAllByMemberId(
            memberId,
            RefreshToken.REVOKE_REASON_PASSWORD_RESET
        );
    }
}
