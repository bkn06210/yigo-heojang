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
    private final RefreshTokenMapper refreshTokenMapper;
    private final TokenHashUtil tokenHashUtil;

    @Transactional
    public void replace(Long memberId, String refreshToken, LocalDateTime expiresAt) {
        refreshTokenMapper.revokeAllByMemberId(
            memberId,
            RefreshToken.REVOKE_REASON_REISSUED);

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

    // 회원 탈퇴 시 이 회원의 모든 refresh token을 폐기한다.
    @Transactional
    public void revokeAllByWithdrawal(Long memberId) {
        refreshTokenMapper.revokeAllByMemberId(
            memberId,
            RefreshToken.REVOKE_REASON_MEMBER_WITHDRAWN
        );
    }
}