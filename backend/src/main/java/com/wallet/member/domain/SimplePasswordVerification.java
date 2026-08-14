package com.wallet.member.domain;

import java.time.LocalDateTime;

import com.wallet.auth.domain.VerificationStatus;

// simple_password_verification 테이블 한 행에 대응하는 도메인 객체.
// 간편비밀번호를 설정·변경하기 전에 거치는 이메일 인증의 상태를 담는다.
// 인증 코드와 변경 토큰은 원문이 아니라 SHA-256 해시만 보관한다.
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

    public Long getSimplePasswordVerificationId() {
        return simplePasswordVerificationId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getVerificationCodeHash() {
        return verificationCodeHash;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public Integer getFailedAttemptCount() {
        return failedAttemptCount;
    }

    public LocalDateTime getVerificationCodeExpiresAt() {
        return verificationCodeExpiresAt;
    }

    public String getChangeTokenHash() {
        return changeTokenHash;
    }

    public LocalDateTime getChangeTokenExpiresAt() {
        return changeTokenExpiresAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
