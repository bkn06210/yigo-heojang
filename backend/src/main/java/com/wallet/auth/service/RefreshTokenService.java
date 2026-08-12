package com.wallet.auth.service;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.auth.domain.RefreshToken;
import com.wallet.auth.mapper.RefreshTokenMapper;
import com.wallet.common.util.Sha256Hasher;

@RequiredArgsConstructor
@Service
public class RefreshTokenService {
    private final RefreshTokenMapper refreshTokenMapper;
    private final Sha256Hasher sha256Hasher;

    @Transactional
    public void replace(Long memberId, String refreshToken, LocalDateTime expiresAt) {
        refreshTokenMapper.revokeAllByMemberId(
            memberId,
            RefreshToken.REVOKE_REASON_REISSUED);

        String tokenHash = sha256Hasher.sha256(refreshToken);

        refreshTokenMapper.insert(memberId, tokenHash, expiresAt);
    }

    @Transactional(readOnly = true)
    public RefreshToken findValidToken(String refreshToken) {
        String tokenHash = sha256Hasher.sha256(refreshToken);

        return refreshTokenMapper.findValidTokenByHash(tokenHash);
    }

    @Transactional
    public void revokeByToken(String refreshToken) {
        String tokenHash = sha256Hasher.sha256(refreshToken);

        refreshTokenMapper.revokeByHash(
            tokenHash,
            RefreshToken.REVOKE_REASON_LOGOUT
        );
    }

    @Transactional
    public void revokeAllByPasswordReset(Long memberId) {
        refreshTokenMapper.revokeAllByMemberId(
            memberId,
            RefreshToken.REVOKE_REASON_PASSWORD_RESET
        );
    }
}