package com.wallet.auth.domain;

import java.time.LocalDateTime;

public class RefreshToken {
    private Long refreshTokenId;
    private Long memberId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String revokeReason;
    private LocalDateTime createdAt;

    public Long getRefreshTokenId() {
        return refreshTokenId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public String getRevokeReason() {
        return revokeReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
