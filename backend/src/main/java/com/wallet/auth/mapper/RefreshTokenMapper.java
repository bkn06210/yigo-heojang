package com.wallet.auth.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.wallet.auth.domain.RefreshToken;

@Mapper
public interface RefreshTokenMapper {
    void insert(
        @Param("memberId") Long memberId,
        @Param("tokenHash") String tokenHash,
        @Param("expiresAt") LocalDateTime expiresAt
    );

    RefreshToken findValidTokenByHash(@Param("tokenHash") String tokenHash);

    void revokeByHash(
        @Param("tokenHash") String tokenHash,
        @Param("revokeReason") String revokeReason
    );

    void revokeAllByMemberId(
        @Param("memberId") Long memberId,
        @Param("revokeReason") String revokeReason
    );
}