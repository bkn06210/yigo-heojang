package com.wallet.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.wallet.auth.domain.RefreshToken;
import com.wallet.auth.mapper.RefreshTokenMapper;

public class RefreshTokenServiceTest {
    private RefreshTokenMapper refreshTokenMapper;
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenMapper = mock(RefreshTokenMapper.class);

        refreshTokenService = new RefreshTokenService(refreshTokenMapper);
    }

    @Test
    @DisplayName("유효 Refresh Token 조회 - 원문 토큰을 해시로 변환해 조회한다")
    void findValidToken_hashesTokenAndFindsToken() {
        // given
        String refreshToken = "refresh.token.value";
        String expectedHash = sha256(refreshToken);

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
        String expectedHash = sha256(refreshToken);

        // when
        refreshTokenService.revokeByToken(refreshToken);

        // then
        verify(refreshTokenMapper).revokeByHash(expectedHash, "LOGOUT");
    }

    // 테스트 검증용 헬퍼 메서드
    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
