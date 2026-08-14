package com.wallet.member.domain;

import java.time.LocalDateTime;

import lombok.Getter;

import com.wallet.auth.domain.VerificationStatus;

@Getter
public class SimplePasswordVerification {
    private Long simplePasswordVerificationId;
    private Long memberId;
    private String verificationCodeHash;
    private String verificationStatus;
    private Integer failedAttemptCount;
    private LocalDateTime verificationCodeExpiresAt;
    private String changeTokenHash;
    private LocalDateTime changeTokenExpiresAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 이메일 인증을 시작할 때 DB에 저장할 PENDING 상태 객체를 만든다.
     * 인증 코드 원문 대신 단방향 해시만 받도록 하여 원문이 도메인 객체에 오래 남지 않게 한다.
     */
    public static SimplePasswordVerification createPending(
        Long memberId,
        String verificationCodeHash,
        LocalDateTime verificationCodeExpiresAt
    ) {
        SimplePasswordVerification verification = new SimplePasswordVerification();
        verification.memberId = memberId;
        verification.verificationCodeHash = verificationCodeHash;
        verification.verificationStatus = VerificationStatus.PENDING;
        verification.failedAttemptCount = 0;
        verification.verificationCodeExpiresAt = verificationCodeExpiresAt;
        return verification;
    }
}
