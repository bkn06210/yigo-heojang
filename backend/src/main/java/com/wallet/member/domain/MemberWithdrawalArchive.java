package com.wallet.member.domain;

import java.time.LocalDateTime;

// member_withdrawal_archive 테이블 한 행에 대응하는 도메인 객체.
// 탈퇴 시 member 테이블에서 마스킹되어 사라지는 원본 개인정보(이메일·성명)를
// 보관 기간 동안 담아둔다.
public class MemberWithdrawalArchive {
    private Long memberWithdrawalArchiveId;
    private Long memberId;
    private String email;
    private String emailHash;
    private String name;
    private LocalDateTime withdrawnAt;
    private String retentionReason;
    private LocalDateTime createdAt;

    public static MemberWithdrawalArchive of(
        Long memberId,
        String email,
        String emailHash,
        String name,
        LocalDateTime withdrawnAt,
        String retentionReason
    ) {
        MemberWithdrawalArchive archive = new MemberWithdrawalArchive();
        archive.memberId = memberId;
        archive.email = email;
        archive.emailHash = emailHash;
        archive.name = name;
        archive.withdrawnAt = withdrawnAt;
        archive.retentionReason = retentionReason;
        return archive;
    }

    public Long getMemberWithdrawalArchiveId() {
        return memberWithdrawalArchiveId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getEmail() {
        return email;
    }

    public String getEmailHash() {
        return emailHash;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    public String getRetentionReason() {
        return retentionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
