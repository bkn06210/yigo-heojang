package com.wallet.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wallet.auth.mapper.RefreshTokenMapper;

@Service
public class RefreshTokenService {
    private final RefreshTokenMapper refreshTokenMapper;

    public RefreshTokenService(RefreshTokenMapper refreshTokenMapper) {
        this.refreshTokenMapper = refreshTokenMapper;
    }

    @Transactional
    public void replace(Long memberId, String refreshToken, LocalDateTime expiresAt) {
        refreshTokenMapper.revokeAllByMemberId(memberId, "REISSUED");

        String tokenHash = hash(refreshToken);

        refreshTokenMapper.insert(memberId, tokenHash, expiresAt);
    }

    private String hash(String token) {
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
            throw new IllegalStateException("Refresh Token 해시 생성에 실패했습니다.", e);
        }
    }
}