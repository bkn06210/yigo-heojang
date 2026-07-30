package com.wallet.auth.service;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.auth.domain.RefreshToken;
import com.wallet.auth.mapper.RefreshTokenMapper;
import com.wallet.auth.support.TokenHashUtil;

@RequiredArgsConstructor
@Service
public class RefreshTokenService {
    private static final String REVOKE_REASON_LOGOUT = "LOGOUT";
    private static final String REVOKE_REASON_REISSUED = "REISSUED";

    private final RefreshTokenMapper refreshTokenMapper;
    private final TokenHashUtil tokenHashUtil;

    @Transactional
    public void replace(Long memberId, String refreshToken, LocalDateTime expiresAt) {
        refreshTokenMapper.revokeAllByMemberId(memberId, REVOKE_REASON_REISSUED);

        String tokenHash = tokenHashUtil.sha256(refreshToken);

        refreshTokenMapper.insert(memberId, tokenHash, expiresAt);
    }

    @Transactional(readOnly = true)
    public RefreshToken findValidToken(String refreshToken) {
        String tokenHash = tokenHashUtil.sha256(refreshToken);

        return refreshTokenMapper.findValidTokenByHash(tokenHash);
    }

    @Transactional
    public void revokeByToken(String refreshToken) {
        String tokenHash = tokenHashUtil.sha256(refreshToken);

        refreshTokenMapper.revokeByHash(tokenHash, REVOKE_REASON_LOGOUT);
    }
}